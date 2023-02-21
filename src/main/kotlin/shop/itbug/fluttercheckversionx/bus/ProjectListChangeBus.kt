package shop.itbug.fluttercheckversionx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface ProjectListChangeBus {


    fun flutterProjectChangeList(flutterProjectNames: List<String>)

    companion object {
        private val TOPIC  = Topic.create("project list change bus",ProjectListChangeBus::class.java)
        private val messageBus = ApplicationManager.getApplication().messageBus
        /**
         * 发送通知
         */
        fun fire(projectNames: List<String>) {
            messageBus.syncPublisher(TOPIC).flutterProjectChangeList(projectNames)
        }

        /**
         * 监听
         * 注意: 只有在新项目进入才会触发
         */
        fun lisening(flutterProjectNamesCallback: (names: List<String>) -> Unit) {
            messageBus.connect().subscribe(TOPIC,object : ProjectListChangeBus {
                override fun flutterProjectChangeList(flutterProjectNames: List<String>) {
                    flutterProjectNamesCallback.invoke(flutterProjectNames)
                }
            })
        }
    }
}