package de.soderer.utilities.console;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

/** Sample implementation of a low-level keyboard hook on W32 */
public class LowLevelJnaKeyLogger {
	private static boolean quit;
	private static HHOOK hhk;

	public static void main(final String[] args) {
		final boolean handDownInEventChain = false;

		final User32 lib = User32.INSTANCE;
		final HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
		final LowLevelKeyboardProc keyboardHook = new LowLevelKeyboardProc() {
			@Override
			public LRESULT callback(final int nCode, final WPARAM wParam, final KBDLLHOOKSTRUCT info) {
				if (nCode >= 0) {
					switch (wParam.intValue()) {
						case WinUser.WM_KEYUP:
							System.err.println("key down, key=" + info.vkCode);

							if (info.vkCode == 81) {
								quit = true;
							}
							break;
						case WinUser.WM_KEYDOWN:
							System.err.println("key up, key=" + info.vkCode);

							if (info.vkCode == 81) {
								quit = true;
							}
							break;
						case WinUser.WM_SYSKEYUP:
							System.err.println("syskey down, key=" + info.vkCode);

							if (info.vkCode == 81) {
								quit = true;
							}
							break;
						case WinUser.WM_SYSKEYDOWN:
							System.err.println("syskey up, key=" + info.vkCode);

							if (info.vkCode == 81) {
								quit = true;
							}
							break;
						default:
							// do nothing
					}
				}

				if (handDownInEventChain) {
					System.err.println("Executing other hooks");
					final Pointer ptr = info.getPointer();
					final long peer = Pointer.nativeValue(ptr);
					return lib.CallNextHookEx(hhk, nCode, wParam, new LPARAM(peer));
				} else {
					return new LRESULT(-1);
				}
			}
		};
		hhk = lib.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0);
		System.out.println("Keyboard hook installed, type anywhere, 'q' to quit");
		new Thread() {
			@Override
			public void run() {
				while (!quit) {
					try {
						Thread.sleep(10);
					} catch (@SuppressWarnings("unused") final Exception e) {
						// do nothing
					}
				}
				System.err.println("unhook and exit");
				lib.UnhookWindowsHookEx(hhk);
				System.exit(0);
			}
		}.start();

		// This bit never returns from GetMessage
		int result;
		final MSG msg = new MSG();
		while ((result = lib.GetMessage(msg, null, 0, 0)) != 0) {
			if (result == -1) {
				System.err.println("error in get message");
				break;
			} else {
				System.err.println("got message");
				lib.TranslateMessage(msg);
				lib.DispatchMessage(msg);
			}
		}
		lib.UnhookWindowsHookEx(hhk);
	}
}
