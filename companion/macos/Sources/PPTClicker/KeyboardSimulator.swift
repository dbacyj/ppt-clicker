import CoreGraphics

/// 通过 CGEvent 模拟键盘事件。
/// 逻辑键名 → macOS keyCode 映射对应 protocol/spec.md §6。
enum KeyboardSimulator {

    /// 逻辑键名 → macOS virtual keyCode（十进制）。
    /// 来源：HIToolbox/Events.h kVK_* 常量。
    private static let keyMap: [String: CGKeyCode] = [
        "page_up":     116,   // kVK_PageUp
        "page_down":   121,   // kVK_PageDown
        "arrow_left":  123,   // kVK_LeftArrow
        "arrow_right": 124,   // kVK_RightArrow
        "space":        49,   // kVK_Space
        "enter":        36,   // kVK_Return
        "escape":       53,   // kVK_Escape
        "f5":           96,   // kVK_F5
        "b":            11,   // kVK_ANSI_B
        "period":       47,   // kVK_ANSI_Period
        "home":        115,   // kVK_Home
        "end":         119,   // kVK_End
        "shift":        56,   // kVK_Shift
        "ctrl":         59,   // kVK_Control
        "alt":          58,   // kVK_Option
    ]

    /// 触发一次按键：按下 → 抬起。
    /// 注意：辅助进程需要有"辅助功能"权限（系统设置 → 隐私 → 辅助功能）。
    static func send(_ code: String) -> Bool {
        if code == "shift_f5" {
            return sendCombo(["shift", "f5"])
        }
        guard let keyCode = keyMap[code] else { return false }
        postKey(keyCode, down: true)
        postKey(keyCode, down: false)
        return true
    }

    /// 组合键：依次按下 modifiers → 主键 → 逆序抬起。
    static func sendCombo(_ codes: [String]) -> Bool {
        let keyCodes = codes.compactMap { keyMap[$0] }
        guard keyCodes.count == codes.count else { return false }

        keyCodes.forEach { postKey($0, down: true) }
        keyCodes.reversed().forEach { postKey($0, down: false) }
        return true
    }

    private static func postKey(_ keyCode: CGKeyCode, down: Bool) {
        let event = CGEvent(
            keyboardEventSource: CGEventSource(stateID: .hidSystemState),
            virtualKey: keyCode,
            keyDown: down
        )
        event?.post(tap: .cghidEventTap)
    }
}
