package com.pptclicker.hid

/**
 * 翻页逻辑键名。与协议 protocol/spec.md §6 保持一致。
 *
 * 蓝牙 HID 模式下手机只是一个"哑键盘"，无法获取 PPT 状态，
 * 故这里只覆盖纯按键操作。状态获取走 WiFi 伴侣（见 [com.pptclicker.net]）。
 */
enum class ClickerKey(val code: String) {
    PAGE_UP("page_up"),
    PAGE_DOWN("page_down"),
    ARROW_LEFT("arrow_left"),
    ARROW_RIGHT("arrow_right"),
    SPACE("space"),
    ENTER("enter"),
    ESCAPE("escape"),
    F5("f5"),
    SHIFT_F5("shift_f5"),
    B("b"),
    PERIOD("period"),
    HOME("home"),
    END("end");

    companion object {
        fun fromCode(code: String): ClickerKey? = entries.firstOrNull { it.code == code }
    }
}
