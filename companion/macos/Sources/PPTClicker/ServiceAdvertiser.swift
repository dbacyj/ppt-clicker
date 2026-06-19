import Foundation
import Network

/// 用 NetService（Bonjour）在局域网广播本服务，供安卓 NsdManager 自动发现。
/// 服务类型对应 protocol/spec.md §8。
final class ServiceAdvertiser: NSObject, NetServiceDelegate {

    private var service: NetService?
    private let name: String
    private let port: Int32
    private let txtDict: [String: String]

    init(hostName: String, port: Int32) {
        self.name = "PPTClicker-\(hostName)"
        self.port = port
        self.txtDict = [
            "v": "1",
            "port": "\(port)",
            "platform": "macos",
            "name": hostName,
        ]
        super.init()
    }

    func start() {
        let service = NetService(domain: "", type: ProtocolSpec.serviceType, name: name, port: port)
        service.delegate = self
        service.setTXTRecord(NetService.data(fromTXTRecord: txtDict))
        service.publish()
        self.service = service
        print("[mDNS] 广播 \(name).\(ProtocolSpec.serviceType)")
    }

    func stop() {
        service?.stop()
        service = nil
    }
}
