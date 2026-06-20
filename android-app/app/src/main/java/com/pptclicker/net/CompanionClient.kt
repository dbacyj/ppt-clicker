package com.pptclicker.net

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

/**
 * WiFi 模式下的连接状态。
 */
sealed class WifiState {
    data object Idle : WifiState()
    data class Connecting(val host: String) : WifiState()
    /** 已建立 TCP/WS，但未完成协议握手（hello/hello_ack）。 */
    data object Handshaking : WifiState()
    /** 已认证，可发送按键。 */
    data class Connected(val serverName: String) : WifiState()
    data class Error(val message: String) : WifiState()
    data object Disconnected : WifiState()
}

/**
 * 电脑伴侣程序的 WebSocket 客户端。
 *
 * 内部封装协议握手（hello/hello_ack）、按键下发、心跳。
 * 调用方只需 [connect] / [sendKey] / [disconnect]。
 *
 * 注意：BluetoothHidDevice 不可用时（国产 ROM），翻页通过本类走伴侣程序。
 */
class CompanionClient(
    private val clientName: String,
    private val appVersion: String,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val idSeq = AtomicInteger(0)
    private var ws: Client? = null
    @Volatile private var handshaked = false
    private var pingThread: Thread? = null

    var onStateChanged: ((WifiState) -> Unit)? = null

    /** 默认空配对码（伴侣可关闭校验）。 */
    var pairCode: String = ""

    private fun nextId(prefix: String): String = "${prefix}${idSeq.incrementAndGet()}"

    fun connect(host: String, port: Int = Protocol.DEFAULT_PORT) {
        disconnect()
        notify(WifiState.Connecting(host))
        val uri = URI("ws://$host:$port/")
        ws = Client(uri).also { it.connect() }
    }

    fun disconnect() {
        stopPing()
        handshaked = false
        try { ws?.close() } catch (_: Throwable) {}
        ws = null
    }

    /**
     * 发送按键。返回 false 表示未连接/未握手。
     * 成功返回不代表主机已执行，结果经 [onKeyResult] 异步通知。
     */
    fun sendKey(code: String): Boolean {
        val w = ws ?: return false
        if (!handshaked) return false
        val msg = Protocol.key(code, nextId("k"))
        return try { w.send(msg); true } catch (e: Exception) { false }
    }

    /** 按键执行结果回调（对应 key_result）。 */
    var onKeyResult: ((id: String, ok: Boolean) -> Unit)? = null

    private fun handleOpen() {
        notify(WifiState.Handshaking)
        // 握手：发送 hello
        ws?.send(Protocol.hello(pairCode, clientName, appVersion))
    }

    private fun handleMessage(raw: String) {
        val json = try { JSONObject(raw) } catch (e: Exception) {
            Log.w(TAG, "bad json: $raw"); return
        }
        when (json.optString("type")) {
            "hello_ack" -> {
                handshaked = true
                val name = json.optJSONObject("data")
                    ?.optJSONObject("server")?.optString("name") ?: "Server"
                startPing()
                notify(WifiState.Connected(name))
            }
            "hello_nack" -> {
                val reason = json.optJSONObject("data")?.optString("reason") ?: "rejected"
                notify(WifiState.Error("握手被拒: $reason"))
                disconnect()
            }
            "key_result" -> {
                val data = json.optJSONObject("data")
                val ok = data?.optBoolean("ok", false) ?: false
                onKeyResult?.invoke(json.optString("id"), ok)
            }
            "pong" -> { /* 心跳响应，无需处理 */ }
            "error" -> {
                val code = json.optJSONObject("data")?.optString("code") ?: "internal"
                Log.w(TAG, "server error: $code")
            }
            else -> Log.d(TAG, "ignored: ${json.optString("type")}")
        }
    }

    private fun handleClose(code: Int, reason: String?) {
        stopPing()
        handshaked = false
        ws = null
        notify(if (code == 1000) WifiState.Disconnected
               else WifiState.Error("连接关闭($code): ${reason ?: ""}"))
    }

    private fun handleError(ex: Exception) {
        notify(WifiState.Error(ex.message ?: ex.javaClass.simpleName))
    }

    private fun startPing() {
        stopPing()
        pingThread = Thread {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_MS)
                    val w = ws ?: break
                    if (w.isOpen) w.send(Protocol.ping(nextId("p"), System.currentTimeMillis()))
                } catch (_: InterruptedException) { break
                } catch (_: Exception) { break }
            }
        }.apply { isDaemon = true; name = "pptclicker-ping"; start() }
    }

    private fun stopPing() {
        pingThread?.interrupt(); pingThread = null
    }

    private fun notify(state: WifiState) {
        handler.post { onStateChanged?.invoke(state) }
    }

    private inner class Client(uri: URI) : WebSocketClient(uri) {
        override fun onOpen(handshaked: ServerHandshake?) = this@CompanionClient.handleOpen()
        override fun onMessage(message: String?) = message?.let { this@CompanionClient.handleMessage(it) } ?: Unit
        override fun onClose(code: Int, reason: String?, remote: Boolean) =
            this@CompanionClient.handleClose(code, reason)
        override fun onError(ex: Exception?) = this@CompanionClient.handleError(ex ?: Exception("unknown"))
    }

    companion object {
        private const val TAG = "CompanionClient"
        private const val HEARTBEAT_MS = 15_000L
    }
}
