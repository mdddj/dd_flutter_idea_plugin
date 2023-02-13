package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.dialog.FreezedCovertDialog
import shop.itbug.fluttercheckversionx.model.DartClassProperty
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.util.firstChatToUpper

/**
 * 判断用户选中的是否为 class method 名字节点
 */
fun AnActionEvent.selectTextIsDartMethodElement(): Boolean {
    val element = getData(CommonDataKeys.PSI_ELEMENT)
    element?.let {
        return it is DartComponentNameImpl
    }
    return false
}

fun DartNormalFormalParameterImpl.covertDartPropertyModel(): DartClassProperty {
    return DartClassProperty(type = firstChild.firstChild.text, name = firstChild.lastChild.text, isNonNull = false)
}

fun DartOptionalFormalParametersImpl.covertDartPropertyModels(): MutableList<DartClassProperty> {
    val childrenOfType = PsiTreeUtil.getChildrenOfType(this, DartDefaultFormalNamedParameterImpl::class.java)
    val properties = mutableListOf<DartClassProperty>()
    childrenOfType?.forEach {
        properties.add(
            DartClassProperty(
                type = it.firstChild.firstChild.firstChild.text,
                name = it.firstChild.firstChild.lastChild.text, isNonNull = true
            )
        )
    }
    return properties
}

/**
 * 参数转freezed model
 */
class FunctionParamsToFreezed : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val element = e.getData(CommonDataKeys.PSI_ELEMENT)!! as DartComponentNameImpl
        val paramsElement = element.nextSibling as DartFormalParameterListImpl

        val reqParamsElement = PsiTreeUtil.getChildrenOfType(paramsElement, DartNormalFormalParameterImpl::class.java)
        val optParamsElement =
            PsiTreeUtil.getChildrenOfType(paramsElement, DartOptionalFormalParametersImpl::class.java)

        val properties = mutableListOf<DartClassProperty>()
        reqParamsElement?.forEach {
            properties.add(it.covertDartPropertyModel())
        }

        optParamsElement?.forEach {
            properties.addAll(it.covertDartPropertyModels())
        }
        val freezedCovertModel = FreezedCovertModel(
            properties = properties,
            className = (element.text).firstChatToUpper() + "Params",
            isDartClassElementType = true
        )
        FreezedCovertDialog(e.project!!, freezedCovertModel).show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.selectTextIsDartMethodElement()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}