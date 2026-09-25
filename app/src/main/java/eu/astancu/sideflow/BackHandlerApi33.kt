package eu.astancu.sideflow

import android.os.Build
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BackHandlerApi33(private val dispatcher: OnBackInvokedDispatcher, private val callback: OnBackInvokedCallback) {
    init { dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback) }
    fun unregister() { dispatcher.unregisterOnBackInvokedCallback(callback) }
    companion object {
        fun attach(view: View, close: () -> Unit): BackHandlerApi33? {
            val dispatcher = view.findOnBackInvokedDispatcher() ?: return null
            return BackHandlerApi33(dispatcher, OnBackInvokedCallback { close() })
        }
    }
}
