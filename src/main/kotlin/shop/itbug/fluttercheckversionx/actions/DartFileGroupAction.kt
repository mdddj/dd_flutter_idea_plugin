package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ui.FormBuilder
import com.jetbrains.lang.dart.DartFileType
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.icons.MyIcons
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel

/**
 * 合并dart导入
 */
class DartFileGroupAction : MyDumbAwareAction() {

    private val fileNameTextFiled: ExtendableTextField = ExtendableTextField()
    private val sb: java.lang.StringBuilder = java.lang.StringBuilder()
    private lateinit var project: Project
    private lateinit var virtualFile: VirtualFile
    private val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(showForm, null)
        .setRequestFocus(true)
        .setTitle("文件名")
        .setCancelKeyEnabled(true)
        .setResizable(true)
        .setMovable(true)
        .createPopup()

    init {
        fileNameTextFiled.border = BorderFactory.createEmptyBorder()
        fileNameTextFiled.setExtensions(TextfiledExtends())
        fileNameTextFiled.emptyText.text = "Name"
        fileNameTextFiled.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e != null && fileNameTextFiled.text.isNotEmpty()) {
                    if ((e.keyCode == 10) && (e.keyChar == '\n')) {
                        createFile(fileNameTextFiled.text)
                        popup.cancel(e)
                    }
                }
            }
        })
    }

    override fun actionPerformed(e: AnActionEvent) {
        val pj = e.project
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        pj?.let {
            project = it
            vf?.let {
                virtualFile = vf
                popup.showCenteredInCurrentWindow(project)
            }
        }
    }

    private fun createFile(filename: String) {
        val str = generfileText(virtualFile, sb, "", 0)
        if (str.isNotEmpty()) {
            val psiManager = PsiManager.getInstance(project)
            val psiFileFactory = PsiFileFactory.getInstance(project)
            val findDirectory = psiManager.findDirectory(virtualFile)
            val psiFile =
                psiFileFactory.createFileFromText("$filename.dart", DartFileType.INSTANCE, str.toString())
            runWriteAction {
                findDirectory?.add(psiFile)
            }
        }
    }


    /**
     * 遍历生成一个文件
     * @param file 待检测的文件
     */
    private fun generfileText(file: VirtualFile, sb: StringBuilder, folderName: String, count: Int): StringBuilder {
        if (file.isDirectory) {
            val children = file.children
            children.forEach {
                val name = if (count == 0) "" else "/${file.name}"
                val v = count + 1
                generfileText(it, sb, "$folderName$name", v)
            }
        } else {
            val isDart = file.presentableName.contains(".dart")
            if (isDart) {
                sb.append("\n")
                sb.append("export \'.$folderName/${file.presentableName}\';")
            }
        }
        return sb
    }


    private val showForm: JPanel get() = FormBuilder.createFormBuilder().addComponent(fileNameTextFiled).panel


}

///图标
class TextfiledExtends : ExtendableTextComponent.Extension {
    override fun getIcon(hovered: Boolean): Icon {
        return MyIcons.dartPackageIcon
    }

    override fun isIconBeforeText(): Boolean {
        return true
    }

}