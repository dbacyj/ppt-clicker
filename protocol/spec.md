# PPT Clicker — WiFi 通信协议规范 v1

本协议定义安卓 App（客户端）与电脑伴侣程序（服务端）之间的通信约定。
两端都按此实现，协议无关具体编程语言。

## 1. 传输层

- **协议**：WebSocket（RFC 6455），基于 TCP
- **端口**：`48721`（默认；伴侣程序可配置）
- **路径**：`/`（根路径）
- **URL 示例**：`ws://192.168.1.20:48721/`
- **加密**：MVP 不启用 TLS（局域网内，自签证书分发成本高）。P2 版本可引入配对码而非加密。

> 注意：不使用 `wss://`，因为本地伴侣程序没有可信证书。安全性由"同局域网 + 配对码"保证（配对码见 §5）。

## 2. 消息格式

所有消息均为 **JSON 文本帧**（UTF-8），结构统一：

```json
{
  "v": 1,
  "type": "<message-type>",
  "id": "<optional-correlation-id>",
  "data": { ... }
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `v` | int | 是 | 协议版本号，当前为 `1` |
| `type` | string | 是 | 消息类型，见 §3 |
| `id` | string | 否 | 关联 ID，用于请求-响应配对 |
| `data` | object | 视类型 | 消息体 |

收方遇到 `v` 不兼容时应忽略消息并发送 `error`（`code=unsupported_version`）。

## 3. 消息类型总表

| 方向 | type | 说明 |
|------|------|------|
| C→S | `hello` | 连接握手，携带配对码与能力声明 |
| S→C | `hello_ack` | 握手响应，携带伴侣信息 |
| S→C | `hello_nack` | 握手拒绝（配对码错误等） |
| C→S | `key` | 发送一个按键事件（核心翻页指令） |
| C→S | `key_combo` | 发送组合键 |
| S→C | `key_result` | 按键执行结果 |
| C→S | `ping` | 心跳 |
| S→C | `pong` | 心跳响应 |
| S→C | `state` | PPT 状态推送（当前页/总页等，P1 用） |
| 双向 | `error` | 错误通知 |

- C = Client（手机），S = Server（电脑伴侣）
- MVP 必须实现：`hello`/`hello_ack`/`hello_nack`、`key`/`key_result`、`ping`/`pong`、`error`
- `state`/`key_combo` 为 P1，但本规范先行定义

## 4. 详细报文

### 4.1 hello（C→S）

连接建立后由客户端**立即**发送，未收到 `hello_ack` 前服务端不处理任何其他消息。

```json
{
  "v": 1,
  "type": "hello",
  "id": "c1",
  "data": {
    "pair_code": "123456",
    "client": {
      "name": "My Pixel 8",
      "platform": "android",
      "app_version": "0.1.0"
    },
    "caps": ["key", "key_combo"]
  }
}
```

| 字段 | 说明 |
|------|------|
| `pair_code` | 配对码（见 §5）。MVP 可允许伴侣关闭校验 |
| `client.name` | 人类可读设备名 |
| `client.platform` | `android` / `ios` |
| `client.app_version` | App 版本 |
| `caps` | 客户端支持的能力列表 |

### 4.2 hello_ack（S→C）

```json
{
  "v": 1,
  "type": "hello_ack",
  "id": "c1",
  "data": {
    "server": {
      "name": "DESKTOP-ABC",
      "platform": "windows",
      "os_version": "10.0.22631",
      "app_version": "0.1.0"
    },
    "caps": ["key", "key_combo", "state"]
  }
}
```

### 4.3 hello_nack（S→C）

```json
{
  "v": 1,
  "type": "hello_nack",
  "id": "c1",
  "data": { "reason": "pair_code_invalid" }
}
```

`reason` 取值：`pair_code_invalid` / `unsupported_version` / `rejected`。
发完此消息后服务端可主动断开连接。

### 4.4 key（C→S）★ 核心

```json
{
  "v": 1,
  "type": "key",
  "id": "k42",
  "data": { "code": "page_down", "hold_ms": 0 }
}
```

| 字段 | 说明 |
|------|------|
| `code` | 逻辑键名，见 §6 键值表 |
| `hold_ms` | 长按持续时间（毫秒）。`0` = 瞬时点击；`>0` = 模拟按下持续指定时间后释放 |

服务端将逻辑键名映射为该平台对应的物理按键（Win: `SendInput` VK_xxx；macOS: `CGEvent` kVK_xxx）。

### 4.5 key_combo（C→S，P1）

```json
{
  "v": 1,
  "type": "key_combo",
  "id": "k43",
  "data": { "codes": ["ctrl", "shift", "f5"] }
}
```

按顺序按下 modifiers，再按下主键，然后逆序释放。

### 4.6 key_result（S→C）

```json
{
  "v": 1,
  "type": "key_result",
  "id": "k42",
  "data": { "ok": true }
}
```

`ok=false` 时附带 `error.code`。

### 4.7 ping / pong（心跳）

客户端每 `15s` 发 `ping`，服务端立即回 `pong`。任意一方 `30s` 未收到心跳应主动断开。

```json
{ "v": 1, "type": "ping", "id": "p1", "data": { "ts": 1718800000000 } }
{ "v": 1, "type": "pong", "id": "p1", "data": { "ts": 1718800000001 } }
```

### 4.8 state（S→C，P1）

伴侣程序解析 PPT 当前状态后推送：

```json
{
  "v": 1,
  "type": "state",
  "data": {
    "app": "powerpoint",
    "slide_index": 5,
    "slide_count": 24,
    "notes": "第 5 页备注内容...",
    "is_presenting": true
  }
}
```

MVP 伴侣程序**不实现** `state` 推送（不解析 PPT），`caps` 中不含 `state`。
P1 通过 PowerPoint COM / AppleScript 获取。

### 4.9 error（双向）

```json
{
  "v": 1,
  "type": "error",
  "data": { "code": "unknown_key", "message": "code not found: foo" }
}
```

错误码：`unsupported_version` / `bad_message` / `unknown_key` / `not_handshaked` / `internal`。

## 5. 配对码

- 伴侣程序启动时随机生成 6 位数字配对码，显示在主界面/托盘菜单。
- 客户端在 `hello.data.pair_code` 携带，错误返回 `hello_nack`。
- 伴侣提供"跳过配对码"开关（方便 MVP 自分发、降低使用门槛），关闭时 `pair_code` 可为空字符串。

## 6. 键值表（逻辑键名）

| `code` | 含义 | PowerPoint | Keynote | Windows VK | macOS keyCode |
|--------|------|-----------|---------|-----------|---------------|
| `page_up` | 上一页 | 上翻页 | 上翻页 | `VK_PRIOR` (0x21) | kVK_PageUp (116) |
| `page_down` | 下一页 | 下翻页 | 下翻页 | `VK_NEXT` (0x22) | kVK_PageDown (121) |
| `arrow_left` | 左方向 | 上翻页(部分) | 上翻页 | `VK_LEFT` (0x25) | kVK_LeftArrow (123) |
| `arrow_right` | 右方向 | 下翻页(部分) | 下翻页 | `VK_RIGHT` (0x27) | kVK_RightArrow (124) |
| `space` | 空格 | 下翻页/动画 | 下翻页 | `VK_SPACE` (0x20) | kVK_Space (49) |
| `enter` | 回车 | 下翻页 | 下翻页 | `VK_RETURN` (0x0D) | kVK_Return (36) |
| `escape` | Esc | 退出全屏 | 退出全屏 | `VK_ESCAPE` (0x1B) | kVK_Escape (53) |
| `f5` | 开始演示 | Win: 从头演; Mac: 从当前 | F5 | `VK_F5` (0x74) | kVK_F5 (96) |
| `shift_f5` | 从当前页演 | Win: 从当前页 | — | 组合键 | 组合键 |
| `b` | 黑屏 | 黑屏切换 | — | `VK_B` (0x42) | kVK_ANSI_B (11) |
| `period` | 白/黑屏切换 | 切换 | 切换 | `VK_OEM_PERIOD` (0xBE) | kVK_ANSI_Period (47) |
| `home` | 回首页 | 第 1 页 | 第 1 页 | `VK_HOME` (0x24) | kVK_Home (115) |
| `end` | 跳末页 | 末页 | 末页 | `VK_END` (0x23) | kVK_End (119) |

**Modifiers**（用于 key_combo）：`ctrl` / `shift` / `alt` / `cmd`（macOS 的 cmd 映射为 Win 的 ctrl，反之亦然，由服务端处理跨平台语义）。

## 7. 连接生命周期

```
[TCP 连接建立]
   │
[WS 握手完成]
   │
C→S: hello (含 pair_code)
   │
   ├── S→C: hello_ack   → 进入"已认证"状态，可收发业务消息
   │
   └── S→C: hello_nack  → 服务端关闭连接
   │
[C→S: key / ping ...]   ← 业务消息仅在已认证状态处理
[S→C: key_result / pong / state ...]
   │
[任意一方关闭 / 心跳超时] → 连接结束
```

服务端在未认证状态收到非 `hello` 消息时，回 `error{not_handshaked}` 并可断开。

## 8. 服务发现（mDNS / Bonjour）

伴侣程序启动后在局域网广播自己，便于安卓自动发现，免去手输 IP。

- **服务类型**：`_pptclicker._tcp.`
- **服务名**：`PPT Clicker @ <hostname>`（如 `PPT Clicker @ DESKTOP-ABC`）
- **TXT 记录**：
  - `v=1`（协议版本）
  - `port=48721`
  - `platform=windows|macos`
  - `name=<hostname>`

安卓用 `NsdManager` 扫描 `_pptclicker._tcp`，发现后直接尝试 `ws://<ip>:48721/`。
发现失败时 UI 提供手动输入 IP 入口（兜底）。

## 9. 版本演进

- `v` 字段即协议大版本，不兼容变更必须 +1。
- 新增 `type` / `data` 字段属于向后兼容，老实现应忽略未知字段。
- 客户端与伴侣 `caps` 取交集，只用双方都支持的能力。

## 10. MVP 实现清单

| 必做 | 项 |
|------|----|
| ✅ | WebSocket server + client（双方） |
| ✅ | `hello` / `hello_ack` / `hello_nack` |
| ✅ | `key`（含 §6 中的所有基础键）+ `key_result` |
| ✅ | `ping` / `pong` |
| ✅ | `error` |
| ✅ | mDNS 服务发现 |
| ✅ | 配对码（默认开启，可关） |

| 后续 | 项 |
|------|----|
| 🔜 P1 | `key_combo`、`state`（PPT 解析） |
| 🔜 P2 | TLS / `wss`、配对码强化 |
