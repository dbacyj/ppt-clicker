import Foundation
import Network

/// macOS 伴侣程序入口。
///
/// 启动后：
/// 1. 在 0.0.0.0:48721 监听 WebSocket（基于 Network.framework，零外部依赖）
/// 2. Bonjour 广播服务，供手机自动发现
/// 3. 控制台显示配对码与连接日志
///
/// 注意：辅助进程需要有"辅助功能"权限，否则 CGEvent 按键无效。
/// 系统设置 → 隐私与安全性 → 辅助功能 → 添加本程序。
@main
struct PPTClickerMain {

    static func main() {
        let hostName = Host.current().localizedName ?? "Mac"
        let port = ProtocolSpec.defaultPort
        let args = CommandLine.arguments
        let noPair = args.contains("--no-pair")
        let pairCode = noPair ? nil : String(format: "%06d", Int.random(in: 100000...999999))

        printBanner(host: hostName, port: Int(port.rawValue), pair: pairCode)

        let server = WebSocketServer(
            port: port,
            serverName: "PPT Clicker @ \(hostName)",
            expectedPairCode: pairCode
        )
        do {
            try server.start()
        } catch {
            print("[!] 启动失败（端口 \(port.rawValue) 可能被占用）: \(error)")
            print("按回车退出...")
            _ = readLine()
            exit(1)
        }

        let advertiser = ServiceAdvertiser(hostName: hostName, port: Int32(port.rawValue))
        advertiser.start()

        print("\n监听中，等待手机连接。输入 q 回车退出。")
        // 阻塞主线程等待用户退出；WebSocket 回调走 .global() 后台队列，不依赖主 RunLoop
        while readLine() != "q" {}

        advertiser.stop()
        server.stop()
    }

    static func printBanner(host: String, port: Int, pair: String?) {
        let line = String(repeating: "=", count: 48)
        print(line)
        print("  PPT Clicker 伴侣程序  v0.1.0")
        print(line)
        print("  主机名  : \(host)")
        print("  端口    : \(port)")
        print("  配对码  : \(pair ?? "（已关闭校验）")")
        print(String(repeating: "-", count: 48))
        print("  手机端选择 WiFi 模式 → 自动发现本机 → 输入配对码")
        print("  ⚠️ 首次运行请在「系统设置 → 隐私 → 辅助功能」中授权本程序")
        print(line)
        print()
    }
}
