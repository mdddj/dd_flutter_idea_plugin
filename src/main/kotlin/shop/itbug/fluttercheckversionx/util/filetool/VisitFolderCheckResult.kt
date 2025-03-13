package shop.itbug.fluttercheckversionx.util.filetool

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import shop.itbug.fluttercheckversionx.util.HandleVirtualFile

/**
 * 遍历某个目录
 */
class VisitFolderCheckResult(val handleFile: HandleVirtualFile, val skipDirectory: Boolean = true) :
    VirtualFileVisitor<VirtualFile>() {
    override fun visitFileEx(file: VirtualFile): Result {
        if (skipDirectory) {
            if (!file.isDirectory) {
                handleFile(file)
            }
        } else {
            handleFile(file)
        }

        return CONTINUE
    }
}