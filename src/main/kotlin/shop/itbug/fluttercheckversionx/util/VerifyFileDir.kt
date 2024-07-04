package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.layout.ValidationInfoBuilder
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import kotlin.io.path.Path

class VerifyFileDir : DialogValidation.WithParameter<TextFieldWithBrowseButton> {
    override fun curry(parameter: TextFieldWithBrowseButton): DialogValidation {
        println("file: ${parameter.textField.text}")
        return object : DialogValidation {
            override fun validate(): ValidationInfo? {
                val file: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl(parameter.text)
                println("file: -> $file")
                if (file == null || LocalFileSystem.getInstance().exists(file).not()) {
                    return ValidationInfoBuilder(parameter).error(PluginBundle.get("freezed.gen.base.file.dir.error"))
                }
                return null
            }

        }
    }

    companion object {
        fun validateDir(path: String): Pair<Boolean, String>? {
            VirtualFileManager.getInstance().findFileByNioPath(Path(path))
                ?: return Pair(false, PluginBundle.get("freezed.gen.base.file.dir.error"))
            return null
        }

        val ERROR_MSG = PluginBundle.get("freezed.gen.base.file.dir.error")
        val ENTER_YOU_FILE_NAME = PluginBundle.get("freezed.gen.create.enter.you.file.name")
        fun validDirByComponent(comp: TextFieldWithBrowseButton): Boolean {
            val path = comp.text
            return VirtualFileManager.getInstance().findFileByNioPath(Path(path)) == null
        }

        fun validDirByPath(path: String): Boolean {
            return VirtualFileManager.getInstance().findFileByNioPath(Path(path)) == null
        }


    }
}