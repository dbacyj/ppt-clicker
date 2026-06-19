import Foundation
import Network

/// 单个手机连接的会话状态。
private final class ClientSession {
    var handshaked = false
    weak var connection: NWConnection?
}

/// 基于 Network.framework 的 WebSocket server。
///
/// 处理流程：
/// 1. NWListener 接受 TCP 连接，并协商 WebSocket（NWProtocolWebSocket.Options）
/// 2. 每个连接建立一个 ClientSession
/// 3. 收到消息 → 解析协议 → 握手 / 按键 / 心跳
final class WebSocketServer {

    private let port: NWEndpoint.Port
    private let serverName: String
    private let expectedPairCode: String?
    private var listener: NWListener?
    private var sessions: [ObjectIdentifier: ClientSession] = [:]

    init(port: NWEndpoint.Port, serverName: String, expectedPairCode: String?) {
        self.port = port
        self.serverName = serverName
        self.expectedPairCode = expectedPairCode
    }

    func start() throws {
        // 配置 WebSocket 子协议
        let wsOptions = NWProtocolWebSocket.Options()
        wsOptions.autoReplyPing = true

        let tcp = NWProtocolTCP.Options()
        let params = NWParameters(tls: nil, tcp: tcp)
        params.defaultProtocolStack.applicationProtocols.insert(wsOptions, at: 0)

        let listener = try NWListener(using: params, on: port)
        listener.newConnectionHandler = { [weak self] conn in
            self?.handle(conn)
        }
        listener.start(queue: .global(qos: .userInitiated))
        self.listener = listener
        print("[ws] 监听 \(port)")
    }

    func stop() {
        listener?.cancel()
        listener = nil
    }

    // MARK: - 连接处理

    private func handle(_ conn: NWConnection) {
        let session = ClientSession()
        session.connection = conn
        sessions[ObjectIdentifier(conn)] = session
        conn.start(queue: .global(qos: .userInitiated))
        receive(conn, session)
    }

    private func receive(_ conn: NWConnection, _ session: ClientSession) {
        conn.receiveMessage { [weak self] data, _, isComplete, error in
            guard let self = self else { return }
            if let data = data, let text = String(data: data, encoding: .utf8) {
                self.process(text, session, conn)
            }
            if let error = error {
                print("[ws] 收取错误: \(error)")
                self.close(conn); return
            }
            if isComplete { self.close(conn); return }
            // 继续收下一条
            self.receive(conn, session)
        }
    }

    private func process(_ text: String, _ session: ClientSession, _ conn: NWConnection) {
        guard let msg = InboundMessage.parse(text) else {
            send(EnvelopeCodec.error(code: "bad_message", message: "无法解析"), to: conn); return
        }

        if !session.handshaked && msg.type != "hello" {
            send(EnvelopeCodec.error(code: "not_handshaked", message: "请先发送 hello"), to: conn)
            return
        }

        switch msg.type {
        case "hello":  handleHello(msg, session, conn)
        case "key":    handleKey(msg, conn)
        case "ping":   handlePing(msg, conn)
        default:
            send(EnvelopeCodec.error(code: "bad_message", message: "未知 type: \(msg.type)"), to: conn)
        }
    }

    private func handleHello(_ msg: InboundMessage, _ session: ClientSession, _ conn: NWConnection) {
        // 配对码校验
        if let expected = expectedPairCode, !expected.isEmpty {
            let pair = msg.data["pair_code"] as? String
            if pair != expected {
                send(EnvelopeCodec.helloNack(reason: "pair_code_invalid"), to: conn)
                close(conn); return
            }
        }
        session.handshaked = true
        send(EnvelopeCodec.helloAck(serverName: serverName), to: conn)
        print("[+] 客户端已认证")
    }

    private func handleKey(_ msg: InboundMessage, _ conn: NWConnection) {
        let code = msg.data["code"] as? String ?? ""
        let ok = KeyboardSimulator.send(code)
        send(EnvelopeCodec.keyResult(id: msg.id ?? "", ok: ok), to: conn)
        print("[key] \(code) -> \(ok ? "ok" : "fail")")
    }

    private func handlePing(_ msg: InboundMessage, _ conn: NWConnection) {
        let ts = (msg.data["ts"] as? Int64) ?? 0
        send(EnvelopeCodec.pong(id: msg.id ?? "", ts: ts), to: conn)
    }

    private func send(_ text: String, to conn: NWConnection) {
        let metadata = NWProtocolWebSocket.Metadata(opcode: .text)
        let context = NWConnection.ContentContext(identifier: "msg", metadata: [metadata])
        conn.send(content: text.data(using: .utf8), context: context, isComplete: true, completion: .contentProcessed { _ in })
    }

    private func close(_ conn: NWConnection) {
        sessions[ObjectIdentifier(conn)] = nil
        conn.cancel()
    }
}
