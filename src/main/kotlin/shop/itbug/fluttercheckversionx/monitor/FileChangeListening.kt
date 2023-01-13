package shop.itbug.fluttercheckversionx.monitor

import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class FileChangeListening: AsyncFileListener {
    //todo 监听文件变化,重新生成资源对象
    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        println(events.size)
        if(events.isNotEmpty()){
            val f = events.first()
            println(f)
        }
        return ac
    }


   val ac = object : AsyncFileListener.ChangeApplier {

    }

}