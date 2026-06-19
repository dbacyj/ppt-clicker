using Makaretu.DNS;
using PPTClicker.Protocol;

namespace PPTClicker.Net;

/// <summary>
/// 用 mDNS（Bonjour）在局域网广播本服务，供安卓 NsdManager 自动发现。
/// 服务类型对应 protocol/spec.md §8。
/// </summary>
public class ServiceAdvertiser : IDisposable
{
    private readonly ServiceProfile _profile;
    private bool _started;

    public ServiceAdvertiser(string hostName, int port)
    {
        _profile = new ServiceProfile(hostName, ProtocolSpec.ServiceType, (ushort)port);
        // TXT 记录
        _profile.AddProperty("v", "1");
        _profile.AddProperty("port", port.ToString());
        _profile.AddProperty("platform", "windows");
        _profile.AddProperty("name", hostName);
    }

    public void Start()
    {
        if (_started) return;
        ServiceDiscovery sd = new();
        sd.Advertise(_profile);
        _started = true;
        Console.WriteLine($"[mDNS] 广播 {_profile.FullName} port {_profile.Port}");
    }

    public void Dispose()
    {
        // Makaretu 在 GC 时注销；MVP 不做主动注销
        _started = false;
    }
}
