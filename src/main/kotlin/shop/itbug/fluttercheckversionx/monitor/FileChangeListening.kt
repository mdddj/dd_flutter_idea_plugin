package shop.itbug.fluttercheckversionx.monitor

import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class FileChangeListening: AsyncFileListener {
    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        return ac
    }


   val ac = object : AsyncFileListener.ChangeApplier {
   }

}