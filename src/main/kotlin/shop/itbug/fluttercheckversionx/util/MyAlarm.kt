package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.util.Alarm

typealias Listener = (Boolean) -> Unit

class MyAlarm(val disposable: Disposable, val panel: DialogPanel) : Alarm(disposable) {

    fun start(callback: Listener) {
        addRequest({
            val isUpdate = panel.isModified()
            callback.invoke(isUpdate)
            start(callback)
        }, 1000)
    }
}