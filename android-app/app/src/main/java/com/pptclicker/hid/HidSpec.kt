package com.pptclicker.hid

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * HID 相关常量与工具：报告描述符、键位映射、SDP 配置。
 *
 * 报告描述符声明本设备是一个标准引导接口键盘（Boot Keyboard），
 * 这样主机（Win/macOS）无需驱动即可识别。键值用 HID Usage Table (v1.21)。
 */
@RequiresApi(Build.VERSION_CODES.P)
internal object HidSpec {

    /**
     * 标准 HID Boot Keyboard 报告描述符。
     * 输出 Report ID = 1，结构为 [modifier, reserved, key1..key6]（8 字节）。
     */
    val reportDescriptor: ByteArray = byteArrayOf(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x06,        // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85, 0x01,        //   Report ID (1)
        0x05, 0x07,        //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(), //   Usage Minimum (Left Control)
        0x29, 0xE7.toByte(), //   Usage Maximum (Right GUI)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x08,        //   Report Count (8) —— 8 个 modifier 位
        0x81, 0x02,        //   Input (Data,Var,Abs)
        0x95, 0x01,        //   Report Count (1)
        0x75, 0x08,        //   Report Size (8) —— 保留字节
        0x81, 0x01,        //   Input (Cnst,Arr,Abs)
        0x95, 0x05,        //   Report Count (5)
        0x75, 0x01,        //   Report Size (1) —— LED 位
        0x05, 0x08,        //   Usage Page (LEDs)
        0x19, 0x01,        //   Usage Minimum (Num Lock)
        0x29, 0x05,        //   Usage Maximum (Kana)
        0x91.toByte(), 0x02, //   Output (Data,Var,Abs)
        0x95, 0x01,        //   Report Count (1)
        0x75, 0x03,        //   Report Size (3) —— LED 保留位
        0x91.toByte(), 0x01, //   Output (Cnst,Arr,Abs)
        0x95, 0x06,        //   Report Count (6)
        0x75, 0x08,        //   Report Size (8) —— 6 个普通按键
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0xE7.toByte(), //   Logical Maximum (231)
        0x05, 0x07,        //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,        //   Usage Minimum (0)
        0x29, 0xE7.toByte(), //   Usage Maximum (231)
        0x81, 0x00,        //   Input (Data,Arr,Abs)
        0xC0               // End Collection
    )

    /** SDP（服务发现协议）设置：注册为标准键盘。 */
    fun sdpSettings(): BluetoothHidDeviceAppSdpSettings =
        BluetoothHidDeviceAppSdpSettings(
            "PPT Clicker",       // name
            "Android HID Keyboard", // description
            "PPT Clicker",       // provider
            BluetoothHidDevice.SUBCLASS1_KEYBOARD  // subclass
        )

    // —— HID Usage（十进制，源自 USB HID Usage Table v1.21 §10）——

    /** 8 字节键盘报告：[reportId, modifier, reserved, k1, k2, k3, k4, k5, k6] */
    private fun keyReport(modifier: Byte, vararg keys: Byte): ByteArray {
        val r = ByteArray(9)
        r[0] = 1            // Report ID
        r[1] = modifier     // modifier byte
        r[2] = 0            // reserved
        for (i in keys.indices) r[3 + i] = keys[i]
        return r
    }

    /** 全 0 报告 = 松开所有键。 */
    fun releaseReport(): ByteArray = keyReport(0)

    /** 单键按下报告（无 modifier）。 */
    fun pressReport(key: Byte): ByteArray = keyReport(0, key)

    /** 组合键按下报告（带 modifier + 一个主键）。 */
    fun comboReport(modifier: Byte, key: Byte): ByteArray = keyReport(modifier, key)

    // Modifier 位掩码
    const val MOD_LCTRL: Byte = 0x01
    const val MOD_LSHIFT: Byte = 0x02
    const val MOD_LALT: Byte = 0x04
    const val MOD_LGUI: Byte = 0x08

    /**
     * 把 [ClickerKey] 翻译成 HID 报告（按下态）。
     * 返回 null 表示该键在 HID 模式下不可用（如组合键 F5+Shift 需走 [comboReport]）。
     */
    fun pressReportFor(key: ClickerKey): ByteArray? {
        val usage: Byte = when (key) {
            ClickerKey.PAGE_UP    -> 0x4B
            ClickerKey.PAGE_DOWN  -> 0x4E
            ClickerKey.ARROW_LEFT -> 0x50
            ClickerKey.ARROW_RIGHT-> 0x4F
            ClickerKey.SPACE      -> 0x2C
            ClickerKey.ENTER      -> 0x28
            ClickerKey.ESCAPE     -> 0x29
            ClickerKey.F5         -> 0x3E
            ClickerKey.SHIFT_F5   -> return comboReport(MOD_LSHIFT, 0x3E)
            ClickerKey.B          -> 0x05
            ClickerKey.PERIOD     -> 0x37
            ClickerKey.HOME       -> 0x4A
            ClickerKey.END        -> 0x4D
        }
        return pressReport(usage)
    }
}
