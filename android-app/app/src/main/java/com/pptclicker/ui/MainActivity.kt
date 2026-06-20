package com.pptclicker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pptclicker.R
import com.pptclicker.databinding.ActivityMainBinding
import com.pptclicker.hid.ClickerKey
import com.pptclicker.hid.HidController
import com.pptclicker.hid.HidState
import com.pptclicker.net.CompanionClient
import com.pptclicker.net.CompanionDiscovery
import com.pptclicker.net.WifiState

enum class ConnMode { BLUETOOTH, WIFI }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val hid by lazy { HidController(this) }
    private val wifi by lazy {
        CompanionClient(
            clientName = android.os.Build.MODEL,
            appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0"
        )
    }
    private val discovery by lazy { CompanionDiscovery(this) }

    private var mode: ConnMode = ConnMode.BLUETOOTH

    // 蓝牙/位置/附近设备 权限统一申请
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it } && mode == ConnMode.BLUETOOTH) startBluetooth()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindButtons()
        bindHidCallbacks()
        bindWifiCallbacks()

        // 默认进入蓝牙模式；不支持则降级 WiFi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hid.isSupported()) {
            switchMode(ConnMode.BLUETOOTH)
        } else {
            switchMode(ConnMode.WIFI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { hid.stop() } catch (_: Throwable) {}
        try { wifi.disconnect() } catch (_: Throwable) {}
        try { discovery.stopDiscovery() } catch (_: Throwable) {}
    }

    // —— 按钮绑定 ——

    private fun bindButtons() {
        binding.prevBtn.setOnClickListener { send(ClickerKey.PAGE_UP) }
        binding.nextBtn.setOnClickListener { send(ClickerKey.PAGE_DOWN) }
        binding.fullscreenBtn.setOnClickListener { send(ClickerKey.F5) }
        binding.exitBtn.setOnClickListener { send(ClickerKey.ESCAPE) }
        binding.blackBtn.setOnClickListener { send(ClickerKey.B) }
        binding.whiteBtn.setOnClickListener { send(ClickerKey.PERIOD) }
        binding.homeBtn.setOnClickListener { send(ClickerKey.HOME) }
        binding.endBtn.setOnClickListener { send(ClickerKey.END) }
        binding.menuBtn.setOnClickListener { showMenu(it) }
    }

    /** 根据当前模式派发按键。 */
    private fun send(key: ClickerKey) {
        val ok = when (mode) {
            ConnMode.BLUETOOTH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) hid.sendKey(key) else false
            ConnMode.WIFI -> wifi.sendKey(key.code)
        }
        if (!ok) toast(getString(R.string.status_error, "未连接"))
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, R.string.menu_switch_mode)
        popup.menu.add(0, 2, 0, R.string.menu_manual_ip)
        popup.menu.add(0, 3, 0, R.string.menu_pair_code)
        popup.menu.add(0, 4, 0, R.string.menu_about)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> switchMode(if (mode == ConnMode.BLUETOOTH) ConnMode.WIFI else ConnMode.BLUETOOTH)
                2 -> promptIp()
                3 -> promptPairCode()
                4 -> toast("PPT Clicker 0.1.0\nYour phone, your clicker")
            }
            true
        }
        popup.show()
    }

    // —— 模式切换 ——

    @SuppressLint("ObsoleteSdkInt")
    private fun switchMode(newMode: ConnMode) {
        mode = newMode
        // 停掉另一模式
        if (newMode == ConnMode.BLUETOOTH) {
            wifi.disconnect(); discovery.stopDiscovery()
        } else {
            try { hid.stop() } catch (_: Throwable) {}
        }
        binding.modeLabel.text = getString(
            if (newMode == ConnMode.BLUETOOTH) R.string.mode_bluetooth else R.string.mode_wifi
        )
        when (newMode) {
            ConnMode.BLUETOOTH -> startBluetooth()
            ConnMode.WIFI -> startWifiDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetooth() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            setStatusText(getString(R.string.status_unsupported)); return
        }
        val perms = requiredBluetoothPerms()
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permLauncher.launch(missing.toTypedArray()); return
        }
        hid.start()
    }

    private fun startWifiDiscovery() {
        setStatusText(getString(R.string.status_scanning))
        discovery.onFound = { found ->
            runOnUiThread {
                setStatusText(getString(R.string.status_connecting, found.name))
                wifi.connect(found.host, found.port)
            }
        }
        discovery.onError = { msg -> setStatusText(getString(R.string.status_error, msg)) }
        discovery.startDiscovery()
    }

    // —— 回调绑定 ——

    private fun bindHidCallbacks() {
        hid.onStateChanged = { state ->
            runOnUiThread {
                val text = when (state) {
                    is HidState.Unsupported -> getString(R.string.status_unsupported)
                    is HidState.BluetoothOff -> getString(R.string.status_bluetooth_off)
                    is HidState.NoPermission -> getString(R.string.status_no_permission)
                    is HidState.Ready -> getString(R.string.status_ready_bluetooth)
                    is HidState.Connected -> getString(R.string.status_connected_bluetooth)
                    is HidState.Disconnected -> getString(R.string.status_disconnected_bluetooth)
                    is HidState.Error -> getString(R.string.status_error, state.message)
                }
                setStatusText(text)
                // 蓝牙不可用时自动降级到 WiFi
                if (state is HidState.Unsupported || state is HidState.NoPermission) {
                    if (mode == ConnMode.BLUETOOTH) switchMode(ConnMode.WIFI)
                }
            }
        }
    }

    private fun bindWifiCallbacks() {
        wifi.onStateChanged = { state ->
            runOnUiThread {
                val text = when (state) {
                    is WifiState.Idle -> ""
                    is WifiState.Connecting -> getString(R.string.status_connecting, state.host)
                    is WifiState.Handshaking -> getString(R.string.status_handshaking)
                    is WifiState.Connected -> getString(R.string.status_connected_wifi, state.serverName)
                    is WifiState.Disconnected -> getString(R.string.status_disconnected_bluetooth)
                    is WifiState.Error -> getString(R.string.status_error, state.message)
                }
                setStatusText(text)
            }
        }
    }

    // —— 弹窗 ——

    private fun promptIp() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "192.168.1.20"
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_ip_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    discovery.stopDiscovery()
                    setStatusText(getString(R.string.status_connecting, ip))
                    wifi.connect(ip)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun promptPairCode() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "123456"
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_pair_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                wifi.pairCode = input.text.toString().trim()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // —— 工具 ——

    private fun setStatusText(text: String) {
        binding.statusText.text = text
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun requiredBluetoothPerms(): List<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 无需额外权限，BLUETOOTH_SCAN/BLUETOOTH_CONNECT 已覆盖
        }
        return perms
    }
}
