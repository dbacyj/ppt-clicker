using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using PPTClicker.Protocol;
using WebSocketSharp.Server;

namespace PPTClicker.Net;

/// <summary>
/// WebSocket 行为：处理单个手机连接。
/// 负责协议握手（hello/ack）、按键分发（key → key_result）、心跳。
/// </summary>
public class ClickerBehavior : WebSocketBehavior
{
    private bool _handshaked;
    private readonly string _serverName;
    private readonly string _expectedPairCode;

    public ClickerBehavior(string serverName, string expectedPairCode)
    {
        _serverName = serverName;
        _expectedPairCode = expectedPairCode;
    }

    protected override void OnMessage(MessageEventArgs e)
    {
        var env = EnvelopeCodec.Parse(e.Data);
        if (env == null) { Send(EnvelopeCodec.Error("bad_message", "无法解析")); return; }

        // 握手前只接受 hello
        if (!_handshaked && env.Type != "hello")
        {
            Send(EnvelopeCodec.Error("not_handshaked", "请先发送 hello"));
            return;
        }

        switch (env.Type)
        {
            case "hello":   HandleHello(env); break;
            case "key":     HandleKey(env); break;
            case "ping":    HandlePing(env); break;
            default:
                Send(EnvelopeCodec.Error("bad_message", $"未知 type: {env.Type}"));
                break;
        }
    }

    private void HandleHello(Envelope env)
    {
        // 配对码校验（可关闭：ExpectedPairCode 为空时跳过）
        if (!string.IsNullOrEmpty(_expectedPairCode))
        {
            var pair = env.Data.TryGetProperty("pair_code", out var pc) ? pc.GetString() : null;
            if (pair != _expectedPairCode)
            {
                Send(EnvelopeCodec.HelloNack("pair_code_invalid"));
                Sessions.CloseSession(ID);
                return;
            }
        }
        _handshaked = true;
        Send(EnvelopeCodec.HelloAck(_serverName));
        Console.WriteLine($"[+] 客户端已认证: {ID}");
    }

    private void HandleKey(Envelope env)
    {
        var code = env.Data.TryGetProperty("code", out var c) ? c.GetString() : null;
        if (string.IsNullOrEmpty(code))
        {
            Send(EnvelopeCodec.KeyResult(env.Id ?? "", false));
            return;
        }

        bool ok = Input.KeyboardSimulator.Send(code);
        Send(EnvelopeCodec.KeyResult(env.Id ?? "", ok));
        Console.WriteLine($"[key] {code} -> {(ok ? "ok" : "fail")}");
    }

    private void HandlePing(Envelope env)
    {
        long ts = env.Data.TryGetProperty("ts", out var t) ? t.GetInt64() : 0;
        Send(EnvelopeCodec.Pong(env.Id ?? "", ts));
    }

    protected override void OnClose(CloseEventArgs e) =>
        Console.WriteLine($"[-] 客户端断开: {ID} ({e.Code})");

    protected override void OnError(ErrorEventArgs e) =>
        Console.WriteLine($"[!] 错误: {e.Message}");
}
