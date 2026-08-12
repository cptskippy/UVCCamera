package com.serenegiant.usb;

import java.nio.ByteBuffer;

/**
 * Receive UVC device status change notifications from native libuvc.
 *
 * Implementations are invoked from the native capture thread via JNI when the
 * device reports a status change event. Register an implementation with
 * UVCCamera#setStatusCallback to receive events.
 *
 * Usage:
 *     Prerequisites:
 *         - UVCCamera instance must be initialized and native pointer non-zero
 *     Call sequence:
 *         1. UVCCamera#setStatusCallback(callback) — registers callback for future events
 *         2. Callback onStatus is invoked asynchronously on native thread when status changes
 *
 * Thread safety:
 *     - onStatus is called on the native capture thread. Do not perform blocking work.
 *       Marshal to application thread if UI updates are required.
 */
public interface IStatusCallback {
    /**
     * Handle a UVC status change event delivered from native libuvc.
     *
     * Args:
     *     statusClass: UVC status class code identifying the type of status change.
     *     event: Event code for the status change within the class.
     *     selector: Selector identifying the specific entity or control affected.
     *     statusAttribute: Attribute code describing the status attribute value.
     *     data: Direct ByteBuffer containing status data payload. May be empty for some events.
     *
     * Side Effects:
     *     - None directly; implementation decides how to process the event.
     *
     * Code Paths:
     *     1. Callback is invoked for each status change reported by libuvc.
     *        Implementation should inspect statusClass/event to determine action.
     */
    void onStatus(int statusClass, int event, int selector, int statusAttribute, ByteBuffer data);
}
