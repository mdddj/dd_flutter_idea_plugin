package shop.itbug.flutterx.bus

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface DioWindowApiSearchBus {

    fun doSearch(keyword: String)


    companion object {
        private val TOPIC = Topic.create("DioWindowApiSearchKeyWorld", DioWindowApiSearchBus::class.java)

        val bus = ApplicationManager.getApplication().messageBus
        fun fire(keyword: String) {
            bus.syncPublisher(TOPIC).doSearch(keyword)
        }

        /**
         * 监听搜索接口过滤
         */
        fun listing(parentDisposable: Disposable, search: (keyword: String) -> Unit) {
            bus.connect(parentDisposable).subscribe(TOPIC, object : DioWindowApiSearchBus {
                override fun doSearch(keyword: String) {
                    search.invoke(keyword)
                }
            })
        }
    }

}