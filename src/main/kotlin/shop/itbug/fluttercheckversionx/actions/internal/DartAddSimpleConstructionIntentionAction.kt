package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.lang.dart.psi.DartClassDefinition
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import javax.swing.Icon
import javax.swing.SwingUtilities

class DartAddSimpleConstructionIntentionAction : PsiElementBaseIntentionAction(), Iconable {
    override fun invoke(
        project: Project,
        editor: Editor,
        element: PsiElement
    ) {
        if (editor is ImaginaryEditor) return
        val classEle = PsiTreeUtil.findFirstParent(element) { it is DartClassDefinitionImpl } as? DartClassDefinitionImpl ?: return
        val manager = classEle.myManagerFun()
        val caretModel = editor.caretModel
        val visualPosition = caretModel.visualPosition
        val point = editor.visualPositionToXY(visualPosition)
        val relativePoint = RelativePoint(editor.contentComponent, point)
        SwingUtilities.invokeLater {
            manager.showChooseConstructorPopup(relativePoint) {
                manager.addSimpleFactory(it)
            }
        }

    }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ): Boolean {
        if(!PluginConfig.getState(project).enableFreezedIntentionActions) return false


        if (editor is ImaginaryEditor) return false
        return PsiTreeUtil.findFirstParent(element,{it is DartClassDefinition}) != null
    }

    override fun getFamilyName(): @IntentionFamilyName String {
        return "Convert to simple constructor"
    }

    override fun getIcon(flags: Int): Icon {
        return MyIcons.flutter
    }

    override fun getText(): @IntentionName String {
        return familyName
    }
}