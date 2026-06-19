using System.Text.Json;
using System.Text.Json.Serialization;

namespace PPTClicker.Protocol;

/// <summary>
/// 协议常量，对应 protocol/spec.md。
/// 安卓客户端与本服务端共用同一份 JSON 结构。
/// </summary>
public static class ProtocolSpec
{
    public const int Version = 1;
    public const int DefaultPort = 48721;
    public const string ServiceType = "_pptclicker._tcp";
    public const int HeartbeatMs = 15000;
}

/// <summary>统一的协议信封。</summary>
public class Envelope
{
    [JsonPropertyName("v")]
    public int Version { get; set; } = ProtocolSpec.Version;

    [JsonPropertyName("type")]
    public string Type { get; set; } = "";

    [JsonPropertyName("id")]
    public string? Id { get; set; }

    [JsonPropertyName("data")]
    public JsonElement Data { get; set; }
}

public static class EnvelopeCodec
{
    private static readonly JsonSerializerOptions Opt = new()
    {
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    public static Envelope? Parse(string raw) =>
        JsonSerializer.Deserialize<Envelope>(raw, Opt);

    public static string HelloAck(string serverName) => Build("hello_ack", new
    {
        server = new { name = serverName, platform = "windows", app_version = "0.1.0" },
        caps = new[] { "key", "key_combo" }
    });

    public static string HelloNack(string reason) => Build("hello_nack", new { reason });

    public static string KeyResult(string id, bool ok) =>
        Build("key_result", new { ok }, id);

    public static string Pong(string id, long ts) =>
        Build("pong", new { ts }, id);

    public static string Error(string code, string message) =>
        Build("error", new { code, message });

    private static string Build(string type, object data, string? id = null) =>
        JsonSerializer.Serialize(new
        {
            v = ProtocolSpec.Version,
            type,
            id,
            data
        }, Opt);
}
