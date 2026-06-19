using System.Net;
using PPTClicker.Net;
using PPTClicker.Protocol;
using WebSocketSharp.Server;

namespace PPTClicker;

/// <summary>
/// Windows 伴侣程序入口。
///
/// 启动后：
/// 1. 在 0.0.0.0:48721 监听 WebSocket
/// 2. mDNS 广播服务，供手机自动发现
/// 3. 控制台窗口显示配对码与连接日志
///
/// MVP 采用控制台形态（开发期最简）。后续可加 NotifyIcon 托盘常驻。
/// </summary>
public static class Program
{
    [STAThread]
    public static void Main(string[] args)
    {
        var hostName = Environment.MachineName;
        var port = ProtocolSpec.DefaultPort;

        // 配对码：MVP 默认随机生成 6 位，可通过 --no-pair 关闭校验
        var pairCode = args.Contains("--no-pair") ? "" : Random.Shared.Next(100000, 999999).ToString();

        Console.Title = "PPT Clicker 伴侣程序";
        PrintBanner(hostName, port, pairCode);

        var http = new HttpListener();
        // 注意：WebSocketSharp 的 wssv 自己监听，HttpListener 这里只用于端口预检
        var wssv = new WebSocketServer(IPAddress.Any, port);
        wssv.AddWebSocketService("/",
            () => new ClickerBehavior($"PPT Clicker @ {hostName}", pairCode));

        try
        {
            wssv.Start();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"[!] 启动失败（端口 {port} 可能被占用/防火墙拦截）: {ex.Message}");
            Console.WriteLine("    提示：管理员运行 netsh http add urlacl，或在防火墙放行 {port} 端口");
            Console.WriteLine("按任意键退出...");
            Console.ReadKey();
            return;
        }

        using var advertiser = new ServiceAdvertiser($"PPTClicker-{hostName}", port);
        advertiser.Start();

        Console.WriteLine("\n监听中，等待手机连接。输入 q 回车退出。");
        while (Console.ReadLine() != "q") { }

        wssv.Stop();
    }

    private static void PrintBanner(string host, int port, string pair)
    {
        Console.WriteLine(new string('=', 48));
        Console.WriteLine("  PPT Clicker 伴侣程序  v0.1.0");
        Console.WriteLine(new string('=', 48));
        Console.WriteLine($"  主机名  : {host}");
        Console.WriteLine($"  端口    : {port}");
        Console.WriteLine($"  配对码  : {(string.IsNullOrEmpty(pair) ? "（已关闭校验）" : pair)}");
        Console.WriteLine(new string('-', 48));
        Console.WriteLine("  手机端选择 WiFi 模式 → 自动发现本机 → 输入配对码");
        Console.WriteLine(new string('=', 48));
        Console.WriteLine();
    }
}
