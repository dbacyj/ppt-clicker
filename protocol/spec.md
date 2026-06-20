# PPT Clicker — WiFi Protocol Specification v1

This document defines the communication contract between the Android app (client) and the desktop companion program (server). Both sides implement against it; it is language-agnostic.

## 1. Transport

- **Protocol**: WebSocket (RFC 6455) over TCP
- **Port**: `48721` (default; companion is configurable)
- **Path**: `/` (root)
- **Example URL**: `ws://192.168.1.20:48721/`
- **Encryption**: No TLS in the MVP (local network; self-signed certs are painful to distribute). Security is provided by "same LAN + pairing code" (see §5). TLS / `wss` is a future enhancement.

## 2. Message format

All messages are **JSON text frames** (UTF-8) with a uniform envelope:

```json
{
  "v": 1,
  "type": "<message-type>",
  "id": "<optional-correlation-id>",
  "data": { ... }
}
```

| Field | Type | Required | Description |
|------|------|----------|-------------|
| `v` | int | yes | Protocol version, currently `1` |
| `type` | string | yes | Message type, see §3 |
| `id` | string | no | Correlation id for request/response pairing |
| `data` | object | depends | Message body |

A receiver that encounters an unsupported `v` should ignore the message and emit `error` (`code=unsupported_version`).

## 3. Message types

| Direction | type | Description |
|-----------|------|-------------|
| C→S | `hello` | Connection handshake with pairing code and capabilities |
| S→C | `hello_ack` | Handshake accepted with server info |
| S→C | `hello_nack` | Handshake rejected (wrong pairing code, etc.) |
| C→S | `key` | Send a single key event (the core command) |
| C→S | `key_combo` | Send a key combination |
| S→C | `key_result` | Result of a key event |
| C→S | `ping` | Heartbeat |
| S→C | `pong` | Heartbeat reply |
| S→C | `state` | PPT state push (current slide, total slides — P1) |
| both | `error` | Error notification |

- C = Client (phone), S = Server (companion)
- **MVP-required**: `hello`/`hello_ack`/`hello_nack`, `key`/`key_result`, `ping`/`pong`, `error`
- `state`/`key_combo` are P1 but specified here for forward compatibility

## 4. Detailed messages

### 4.1 hello (C→S)

Sent by the client **immediately** after the connection is established. The server ignores all other messages until `hello_ack` is sent.

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

| Field | Description |
|------|-------------|
| `pair_code` | Pairing code (see §5). The companion may disable verification in MVP |
| `client.name` | Human-readable device name |
| `client.platform` | `android` / `ios` |
| `client.app_version` | App version |
| `caps` | Capabilities the client supports |

### 4.2 hello_ack (S→C)

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

### 4.3 hello_nack (S→C)

```json
{
  "v": 1,
  "type": "hello_nack",
  "id": "c1",
  "data": { "reason": "pair_code_invalid" }
}
```

`reason` values: `pair_code_invalid` / `unsupported_version` / `rejected`.
After sending this, the server may close the connection.

### 4.4 key (C→S) — core

```json
{
  "v": 1,
  "type": "key",
  "id": "k42",
  "data": { "code": "page_down", "hold_ms": 0 }
}
```

| Field | Description |
|------|-------------|
| `code` | Logical key name, see §6 |
| `hold_ms` | Hold duration in milliseconds. `0` = instant tap; `>0` = press, hold, then release |

The server maps the logical key name to the platform's physical key (Win: `SendInput` VK_xxx; macOS: `CGEvent` kVK_xxx).

### 4.5 key_combo (C→S, P1)

```json
{
  "v": 1,
  "type": "key_combo",
  "id": "k43",
  "data": { "codes": ["ctrl", "shift", "f5"] }
}
```

Modifiers are pressed in order, then the main key, then released in reverse order.

### 4.6 key_result (S→C)

```json
{
  "v": 1,
  "type": "key_result",
  "id": "k42",
  "data": { "ok": true }
}
```

When `ok=false`, `error.code` is attached.

### 4.7 ping / pong (heartbeat)

The client sends `ping` every `15s`; the server replies immediately with `pong`. Either side should disconnect if no heartbeat is received for `30s`.

```json
{ "v": 1, "type": "ping", "id": "p1", "data": { "ts": 1718800000000 } }
{ "v": 1, "type": "pong", "id": "p1", "data": { "ts": 1718800000001 } }
```

### 4.8 state (S→C, P1)

Pushed by the companion after parsing the PPT's current state:

```json
{
  "v": 1,
  "type": "state",
  "data": {
    "app": "powerpoint",
    "slide_index": 5,
    "slide_count": 24,
    "notes": "Notes for slide 5...",
    "is_presenting": true
  }
}
```

The MVP companion does **not** implement `state` (it does not parse PPTs); `caps` will not include `state`. P1 obtains it via PowerPoint COM / AppleScript.

### 4.9 error (both directions)

```json
{
  "v": 1,
  "type": "error",
  "data": { "code": "unknown_key", "message": "code not found: foo" }
}
```

Error codes: `unsupported_version` / `bad_message` / `unknown_key` / `not_handshaked` / `internal`.

## 5. Pairing code

- The companion generates a random 6-digit code at startup and displays it in its UI / tray menu.
- The client includes it in `hello.data.pair_code`; a mismatch returns `hello_nack`.
- The companion offers a "skip pairing code" toggle (to ease MVP distribution); when off, `pair_code` may be an empty string.

## 6. Key mapping (logical key names)

| `code` | Meaning | PowerPoint | Keynote | Windows VK | macOS keyCode |
|--------|---------|-----------|---------|-----------|---------------|
| `page_up` | Previous slide | prev | prev | `VK_PRIOR` (0x21) | kVK_PageUp (116) |
| `page_down` | Next slide | next | next | `VK_NEXT` (0x22) | kVK_PageDown (121) |
| `arrow_left` | Left arrow | prev (some) | prev | `VK_LEFT` (0x25) | kVK_LeftArrow (123) |
| `arrow_right` | Right arrow | next (some) | next | `VK_RIGHT` (0x27) | kVK_RightArrow (124) |
| `space` | Space | next/animation | next | `VK_SPACE` (0x20) | kVK_Space (49) |
| `enter` | Enter | next | next | `VK_RETURN` (0x0D) | kVK_Return (36) |
| `escape` | Esc | exit fullscreen | exit fullscreen | `VK_ESCAPE` (0x1B) | kVK_Escape (53) |
| `f5` | Start presentation | Win: from start; Mac: from current | F5 | `VK_F5` (0x74) | kVK_F5 (96) |
| `shift_f5` | Start from current | Win: from current | — | combo | combo |
| `b` | Black screen | toggle black | — | `VK_B` (0x42) | kVK_ANSI_B (11) |
| `period` | Toggle screen | toggle | toggle | `VK_OEM_PERIOD` (0xBE) | kVK_ANSI_Period (47) |
| `home` | First slide | slide 1 | slide 1 | `VK_HOME` (0x24) | kVK_Home (115) |
| `end` | Last slide | last slide | last slide | `VK_END` (0x23) | kVK_End (119) |

**Modifiers** (for `key_combo`): `ctrl` / `shift` / `alt` / `cmd`. (macOS's `cmd` maps to Windows' `ctrl` and vice versa; the server handles cross-platform semantics.)

## 7. Connection lifecycle

```
[TCP connection established]
   │
[WebSocket handshake complete]
   │
C→S: hello (with pair_code)
   │
   ├── S→C: hello_ack   → enters "authenticated" state, business messages allowed
   │
   └── S→C: hello_nack  → server closes connection
   │
[C→S: key / ping ...]   ← business messages only in authenticated state
[S→C: key_result / pong / state ...]
   │
[either side closes / heartbeat timeout] → connection ends
```

If the server receives a non-`hello` message while unauthenticated, it replies with `error{not_handshaked}` and may disconnect.

## 8. Service discovery (mDNS / Bonjour)

The companion advertises itself on the LAN so the Android client can discover it automatically, without manual IP entry.

- **Service type**: `_pptclicker._tcp.`
- **Service name**: `PPT Clicker @ <hostname>` (e.g. `PPT Clicker @ DESKTOP-ABC`)
- **TXT records**:
  - `v=1` (protocol version)
  - `port=48721`
  - `platform=windows|macos`
  - `name=<hostname>`

The Android app scans `_pptclicker._tcp` via `NsdManager`; on discovery it connects to `ws://<ip>:48721/`. If discovery fails, the UI offers manual IP entry as a fallback.

## 9. Versioning

- `v` is the major protocol version; breaking changes bump it.
- Adding new `type`s or `data` fields is backward-compatible; old implementations should ignore unknown fields.
- Clients and companions intersect their `caps` and only use mutually supported capabilities.

## 10. MVP implementation checklist

| Required | Item |
|----------|------|
| ✅ | WebSocket server + client (both sides) |
| ✅ | `hello` / `hello_ack` / `hello_nack` |
| ✅ | `key` (all basic keys in §6) + `key_result` |
| ✅ | `ping` / `pong` |
| ✅ | `error` |
| ✅ | mDNS service discovery |
| ✅ | Pairing code (on by default, disableable) |

| Future | Item |
|--------|------|
| 🔜 P1 | `key_combo`, `state` (PPT parsing) |
| 🔜 P2 | TLS / `wss`, strengthened pairing |
