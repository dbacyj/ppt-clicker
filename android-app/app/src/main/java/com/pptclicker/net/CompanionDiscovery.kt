package com.pptclicker.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * 用 Android NsdManager 扫描局域网内的电脑伴侣程序。
 *
 * 服务类型见 protocol/spec.md §8：`_pptclicker._tcp.`
 * 发现失败时调用方应让用户手动输入 IP（兜底）。
 */
class CompanionDiscovery(context: Context) {

    data class Found(val name: String, val host: String, val port: Int)

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.DiscoveryListener? = null
    private val pending = mutableMapOf<String, NsdServiceInfo>()

    var onFound: ((Found) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun startDiscovery() {
        stopDiscovery()
        listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(info: NsdServiceInfo) {
                // 解析服务以拿到 host/port
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val host = info.host?.hostAddress ?: return
                        onFound?.invoke(Found(info.serviceName, host, info.port))
                    }
                    override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
                        // 单个服务解析失败不影响整体发现
                    }
                })
            }

            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onStartDiscoveryFailed(serviceType: String, code: Int) {
                onError?.invoke("发现启动失败 (code=$code)")
            }
            override fun onStopDiscoveryFailed(serviceType: String, code: Int) {}
        }
        listener?.let { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, it) }
    }

    fun stopDiscovery() {
        listener?.let { l ->
            try { nsd.stopServiceDiscovery(l) } catch (_: Throwable) {}
        }
        listener = null
        pending.clear()
    }

    companion object {
        const val SERVICE_TYPE = "_pptclicker._tcp."
    }
}
