import Foundation

/// 协议常量，对应 protocol/spec.md。安卓客户端与本服务端共用同一份 JSON 结构。
enum ProtocolSpec {
    static let version = 1
    static let defaultPort: NWEndpoint.Port = 48721
    static let serviceType = "_pptclicker._tcp"
    static let heartbeatInterval: TimeInterval = 15
}

/// 协议消息构造器，集中管理 JSON 序列化。
enum EnvelopeCodec {

    // MARK: - 构造响应消息

    static func helloAck(serverName: String) -> String {
        return """
        {"v":1,"type":"hello_ack","data":{"server":{"name":\(quoted(serverName)),"platform":"macos","app_version":"0.1.0"},"caps":["key","key_combo"]}}
        """
    }

    static func helloNack(reason: String) -> String {
        return "{\"v\":1,\"type\":\"hello_nack\",\"data\":{\"reason\":\(quoted(reason)}}"
    }

    static func keyResult(id: String, ok: Bool) -> String {
        return "{\"v\":1,\"type\":\"key_result\",\"id\":\(quoted(id)),\"data\":{\"ok\":\(ok ? "true" : "false")}}"
    }

    static func pong(id: String, ts: Int64) -> String {
        return "{\"v\":1,\"type\":\"pong\",\"id\":\(quoted(id)),\"data\":{\"ts\":\(ts)}}"
    }

    static func error(code: String, message: String) -> String {
        return "{\"v\":1,\"type\":\"error\",\"data\":{\"code\":\(quoted(code)),\"message\":\(quoted(message)}}"
    }

    // MARK: - 解析入站消息

    /// 简易 JSON 字段提取（避免引入完整 JSON 解码器）。
    static func extractString(_ json: [String: Any], _ key: String) -> String? {
        (json[key] as? String)
    }

    private static func quoted(_ s: String) -> String {
        // 简单转义：仅处理引号与反斜杠，足够本协议用
        let escaped = s
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
        return "\"\(escaped)\""
    }
}

/// 用 JSONSerialization 解析入站消息，提取 type / id / data。
struct InboundMessage {
    let type: String
    let id: String?
    let data: [String: Any]

    static func parse(_ raw: String) -> InboundMessage? {
        guard let d = raw.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: d) as? [String: Any]
        else { return nil }
        return InboundMessage(
            type: obj["type"] as? String ?? "",
            id: obj["id"] as? String,
            data: obj["data"] as? [String: Any] ?? [:]
        )
    }
}
