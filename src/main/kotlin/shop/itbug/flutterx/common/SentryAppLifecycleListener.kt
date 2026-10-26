package shop.itbug.flutterx.common

import com.intellij.ide.AppLifecycleListener

/**
 * IDE 退出时刷完 Sentry 队列。
 *
 * `appClosing` 在关闭流程很早就广播，此时后台线程还活着；
 * `appWillBeClosed` 已经太晚，网络线程可能已被回收。
 */
class SentryAppLifecycleListener : AppLifecycleListener {

    override fun appClosing() {
        SentryReportingService.getInstance().flushAndClose()
    }
}
