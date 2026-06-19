using System.Runtime.InteropServices;

namespace PPTClicker.Input;

/// <summary>
/// 通过 Win32 SendInput 模拟键盘事件。
/// 逻辑键名 → Windows VK 映射对应 protocol/spec.md §6。
/// </summary>
public static class KeyboardSimulator
{
    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    private const int INPUT_KEYBOARD = 1;
    private const uint KEYEVENTF_KEYUP = 0x0002;

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public int type;
        public InputUnion u;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public KEYBDINPUT ki;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    /// <summary>逻辑键名 → Windows VK。</summary>
    private static readonly Dictionary<string, ushort> KeyMap = new()
    {
        ["page_up"]     = 0x21,  // VK_PRIOR
        ["page_down"]   = 0x22,  // VK_NEXT
        ["arrow_left"]  = 0x25,
        ["arrow_right"] = 0x27,
        ["space"]       = 0x20,
        ["enter"]       = 0x0D,
        ["escape"]      = 0x1B,
        ["f5"]          = 0x74,
        ["b"]           = 0x42,
        ["period"]      = 0xBE,  // VK_OEM_PERIOD
        ["home"]        = 0x24,
        ["end"]         = 0x23,
        ["ctrl"]        = 0x11,
        ["shift"]       = 0x10,
        ["alt"]         = 0x12,
    };

    /// <summary>触发一次按键：按下 → 抬起。</summary>
    public static bool Send(string code)
    {
        // 组合键 shift+f5 单独处理
        if (code == "shift_f5")
        {
            return SendCombo("shift", "f5");
        }
        if (!KeyMap.TryGetValue(code, out var vk)) return false;

        var inputs = new[]
        {
            KeyDown(vk),
            KeyUp(vk),
        };
        return SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>()) > 0;
    }

    /// <summary>组合键：依次按下 modifiers → 主键 → 逆序抬起。</summary>
    public static bool SendCombo(params string[] codes)
    {
        var vks = codes.Select(c => KeyMap.TryGetValue(c, out var v) ? v : (ushort)0).ToList();
        if (vks.Any(v => v == 0)) return false;

        var down = vks.Select(KeyDown).ToList();
        var up = Enumerable.Reverse(vks).Select(KeyUp);
        var inputs = down.Concat(up).ToArray();
        return SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>()) > 0;
    }

    private static INPUT KeyDown(ushort vk) => new()
    {
        type = INPUT_KEYBOARD,
        u = new InputUnion { ki = new KEYBDINPUT { wVk = vk, dwFlags = 0 } }
    };

    private static INPUT KeyUp(ushort vk) => new()
    {
        type = INPUT_KEYBOARD,
        u = new InputUnion { ki = new KEYBDINPUT { wVk = vk, dwFlags = KEYEVENTF_KEYUP } }
    };
}
