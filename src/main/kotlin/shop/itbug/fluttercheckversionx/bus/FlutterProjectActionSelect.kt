package shop.itbug.fluttercheckversionx.bus

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

interface FlutterProjectActionSelect {


    //执行选择
    fun onSelect(appName:String)

    companion object {
        private val TOPIC = Topic.create("flutter project action select",FlutterProjectActionSelect::class.java)

        //发送
        fun fire(appName: String) {
            ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onSelect(appName)
        }
        //监听
        fun listing(onSelect: (name: String)->Unit) {
            ApplicationManager.getApplication().messageBus.connect().subscribe(TOPIC,object : FlutterProjectActionSelect {
                override fun onSelect(appName: String) {
                    onSelect.invoke(appName)
                }
            })
        }
    }


}