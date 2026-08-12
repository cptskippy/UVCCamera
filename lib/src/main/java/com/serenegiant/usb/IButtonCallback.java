package com.serenegiant.usb;

/**
 * Receive UVC device button press and release events from native libuvc.
 *
 * Implementations are invoked from the native capture thread via JNI when a
 * physical button on the UVC device changes state. Register an implementation
 * with UVCCamera#setButtonCallback to receive events.
 *
 * Usage:
 *     Prerequisites:
 *         - UVCCamera instance must be initialized and native pointer non-zero
 *     Call sequence:
 *         1. UVCCamera#setButtonCallback(callback) — registers callback for future button events
 *         2. Callback onButton is invoked asynchronously on native thread when button state changes
 *
 * Thread safety:
 *     - onButton is called on the native capture thread. Do not perform blocking work.
 *       Marshal to application thread if UI updates are required.
 */
public interface IButtonCallback {
    /**
     * Handle a UVC device button state change.
     *
     * Args:
     *     button: Button identifier reported by the UVC device.
     *     state: Button state value, typically 0 for released and 1 for pressed.
     *
     * Side Effects:
     *     - None directly; implementation decides how to respond to button events.
     *
     * Code Paths:
     *     1. Callback is invoked for each button state change reported by libuvc.
     *        Implementation should interpret button/state to trigger application logic.
     */
    void onButton(int button, int state);
}
