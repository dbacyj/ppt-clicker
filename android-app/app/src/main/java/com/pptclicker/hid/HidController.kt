package com.pptclicker.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.annotation.RequiresApi

/**
 * 连接状态机。蓝牙 HID 模式下手机作 peripheral（被连接方），
 * 需要先让电脑主动配对并连接，状态变化经回调上抛给 UI。
 */
sealed class HidState {
    /** 系统不支持或 ROM 阉割了 BluetoothHidDevice。 */
    data object Unsupported : HidState()
    /** 蓝牙未开启。 */
    data object BluetoothOff : HidState()
    /** 缺少运行时权限（Android 12+ 的 BLUETOOTH_CONNECT 等）。 */
    data object NoPermission : HidState()
    /** 已就绪：HOGP 服务已注册，等待电脑配对连接。 */
    data object Ready : HidState()
    /** 已被电脑连接，可发键。 */
    data object Connected : HidState()
    /** 电脑断开。 */
    data object Disconnected : HidState()
    /** 发生错误。 */
    data class Error(val message: String) : HidState()
}

/**
 * 蓝牙 HID 模块控制器。
 *
 * 使用流程：
 * 1. [start] —— 注册 HOGP 服务，设备对外可见为蓝牙键盘
 * 2. 等待电脑在蓝牙设置里搜索到 "PPT Clicker" 并配对（首次需输 PIN 或自动配对）
 * 3. 电脑连接后进入 [HidState.Connected]，即可 [sendKey]
 * 4. [stop] —— 注销服务，释放资源
 *
 * 国产 ROM（MIUI/EMUI/ColorOS）部分版本阉割了 BluetoothHidDevice，
 * [isSupported] 会探测能力，调用方据此决定是否降级到 WiFi 模式。
 */
@RequiresApi(Build.VERSION_CODES.P)
class HidController(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var appRegistered = false

    /** 状态变化监听。UI 层订阅以更新状态条。 */
    var onStateChanged: ((HidState) -> Unit)? = null

    /**
     * 探测本机是否支持 BluetoothHidDevice（被国产 ROM 阉割时返回 false）。
     *
     * 注意：`getProfileProxy` 的回调成功与否才是真正的能力探测，
     * 这里仅做权限层面的预检，实际能力在 [start] 的代理回调中确认。
     * 若代理回调失败，UI 应据此降级到 WiFi 模式。
     */
    fun isSupported(): Boolean {
        if (adapter == null) return false
        // Android 12+ 需要 BLUETOOTH_CONNECT 运行时权限
        return Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (adapter == null || !isSupported()) {
            notify(HidState.Unsupported); return
        }
        if (!adapter.isEnabled) {
            notify(HidState.BluetoothOff); return
        }
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notify(HidState.NoPermission); return
        }

        // 获取 HID Device profile 代理（异步）
        adapter.getProfileProxy(context, profileServiceListener,
            BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (appRegistered) {
            try { hidDevice?.unregisterApp() } catch (_: Throwable) {}
            appRegistered = false
        }
        adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        connectedDevice = null
        hidDevice = null
        notify(HidState.Disconnected)
    }

    /**
     * 发送一个按键：模拟按下后立即抬起。
     * @return true 表示报告已提交给蓝牙栈（不代表主机已收到）。
     */
    @SuppressLint("MissingPermission")
    fun sendKey(key: ClickerKey): Boolean {
        val dev = connectedDevice ?: return false
        val hid = hidDevice ?: return false
        val press = HidSpec.pressReportFor(key) ?: return false
        // sendReport(reportId, data)：reportId 用我们描述符里定义的 ID=1
        val ok1 = hid.sendReport(dev, 1, press)
        // 松开报告，避免主机认为长按
        hid.sendReport(dev, 1, HidSpec.releaseReport())
        return ok1
    }

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val hid = proxy as? BluetoothHidDevice ?: return
            hidDevice = hid
            hid.registerApp(
                HidSpec.sdpSettings(),       // sdp（含报告描述符）
                /* inQos */ null,
                /* outQos */ null,
                context.mainExecutor,        // executor
                appCallback                  // callback
            )
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                appRegistered = false
                hidDevice = null
                notify(HidState.Disconnected)
            }
        }
    }

    private val appCallback = object : BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            if (registered) notify(HidState.Ready) else notify(HidState.Error("HOGP 未注册成功"))
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    notify(HidState.Connected)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (device == connectedDevice) connectedDevice = null
                    notify(HidState.Disconnected)
                }
                // CONNECTING / DISCONNECTING 忽略
            }
        }
    }

    private fun notify(state: HidState) {
        handler.post { onStateChanged?.invoke(state) }
    }
}
