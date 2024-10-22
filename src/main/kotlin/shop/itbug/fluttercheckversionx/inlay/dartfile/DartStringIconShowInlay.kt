package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartArgumentListImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartVarInitImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import java.io.File
import java.net.URLConnection
import javax.swing.JComponent

/**
 * 检测dart文件中的字符串,或者字符串引用,来显示一个图片
 */
class DartStringIconShowInlay : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("dart.string.icon.showInlay")
    override val name: String
        get() = "Dart Assets Image Show Inlay"
    override val previewText: String?
        get() = """"""

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }

        }
    }

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun getCollectorFor(
        file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {
            val myInlayFactory = HintsInlayPresentationFactory(factory)
            override fun collect(
                element: PsiElement, editor: Editor, sink: InlayHintsSink
            ): Boolean {
                val setting = PluginConfig.getInstance(element.project).state
                if (setting.showAssetsIconInEditor.not()) {
                    return true
                }
                val file = element.checkHasFile() ?: return true
                val inlay = myInlayFactory.getImageWithPath(file.full, file.basePath, setting) ?: return true
                sink.addInlineElement(element.startOffset, false, inlay, false)
                return true
            }
        }
    }

    data class FileResult(
        val file: File, val basePath: String, val full: String
    )

    ///检测是否有本地文件引用
    private fun PsiElement.checkHasFile(): FileResult? {
        fun findFileResult(ele: DartStringLiteralExpressionImpl): FileResult? {
            val dir = ele.project.guessProjectDir() ?: return null
            val url = ele.text.replace("\'", "").replace("\"", "")
            val filePath = dir.path + File.separator + url
            val file = File(filePath)
            if (file.exists() && isImageFile(file)) {
                return FileResult(file, url, filePath)
            }
            return null
        }
        if (this is DartStringLiteralExpressionImpl) {
            return findFileResult(this)
        } else if (reference != null && reference?.resolve() != null && (parent is DartArgumentListImpl || parent is DartVarInitImpl)) {
            val resolvePsi = reference!!.resolve()!!
            if (resolvePsi is DartComponentNameImpl) {
                val findDartStringLiteralInParent = findDartStringLiteralInParent(resolvePsi)
                if (findDartStringLiteralInParent != null) {
                    return findFileResult(findDartStringLiteralInParent)
                }
            }
        }
        return null
    }


    fun isImageFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) {
            return false
        }
        val mimeType = URLConnection.guessContentTypeFromName(file.name)
        return mimeType?.startsWith("image") == true
    }

    fun findDartStringLiteralInParent(element: PsiElement): DartStringLiteralExpressionImpl? {
        val secondParent = element.parent?.parent ?: return null
        return PsiTreeUtil.findChildOfType(secondParent, DartStringLiteralExpressionImpl::class.java)
    }
}