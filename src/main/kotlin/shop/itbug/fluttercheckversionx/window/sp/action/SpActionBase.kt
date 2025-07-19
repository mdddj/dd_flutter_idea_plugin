package shop.itbug.fluttercheckversionx.window.sp.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import shop.itbug.fluttercheckversionx.window.sp.SpWindowLeft
import javax.swing.JComponent
import javax.swing.text.JTextComponent

///
abstract class SpActionBase : AnAction() {


    override fun actionPerformed(p0: AnActionEvent) {
        val key = p0.jlist()!!
        val project = p0.project!!
        action(project, key.selectedValue!!)
    }

    abstract fun action(project: Project, key: String)

    override fun update(e: AnActionEvent) {
        val comp = e.jlist()
        e.presentation.isVisible = comp != null && e.project != null && comp.selectedValue != null
        e.presentation.text = PluginBundle.get("new_value_in_sp_edit") + tip()
        super.update(e)
    }

    abstract fun tip(): String

    fun AnActionEvent.jlist(): SpWindowLeft? {
        return getData(PlatformDataKeys.CONTEXT_COMPONENT) as SpWindowLeft?
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

class SpUpdateStringValueAction : SpActionBase() {
    override fun action(project: Project, key: String) {
        UpdateSpValueDialog(key = key, defaultValue = "", type = "String", inputBuilder = { row, currentValue, update ->
            row.textField().bindText({ "" }, update)
        }, doOk = {
            DioApiService.getInstance().sendByAnyObject(
                mutableMapOf(
                    "action" to "SET_NEW_STRING_VALUE", "key" to key, "newValue" to it
                )
            )
        }).show()

    }

    override fun tip(): String {
        return "(String)"
    }
}

class SpUpdateBoolValueAction : SpActionBase() {
    override fun action(project: Project, key: String) {

        UpdateSpValueDialog(
            key = key,
            defaultValue = false,
            type = "Bool",
            inputBuilder = { panel, curr, update ->
                panel.checkBox("").bindSelected({ curr }, update)
            },
            doOk = {
                DioApiService.getInstance().sendByAnyObject(
                    mutableMapOf(
                        "action" to "SET_NEW_BOOL_VALUE", "key" to key, "newValue" to it
                    )
                )
            }
        ).show()
    }

    override fun tip(): String {
        return "(Bool)"
    }
}


class SpUpdateIntValueAction : SpActionBase() {
    override fun action(project: Project, key: String) {

        UpdateSpValueDialog(
            key = key,
            defaultValue = 0,
            type = "Int",
            inputBuilder = { panel, curr, update ->
                panel.intTextField().bindIntText({ curr }, update)
            },
            doOk = {
                DioApiService.getInstance().sendByAnyObject(
                    mutableMapOf(
                        "action" to "SET_NEW_INT_VALUE", "key" to key, "newValue" to it
                    )
                )
            }
        ).show()
    }

    override fun tip(): String {
        return "(Int)"
    }
}


class SpUpdateDoubleValueAction : SpActionBase() {
    override fun action(project: Project, key: String) {

        UpdateSpValueDialog(
            key = key,
            defaultValue = 0.0,
            type = "Double",
            inputBuilder = { panel, curr, update ->
                panel.cell(JBTextField()).columns(COLUMNS_TINY).validationOnInput {
                    val value = it.text.toDoubleOrNull()
                    when {
                        value == null -> error(PluginBundle.get("please.enter.a.double"))
                        else -> null
                    }
                }.bindDoubleText({ curr }, update)
            },
            doOk = {
                DioApiService.getInstance().sendByAnyObject(
                    mutableMapOf(
                        "action" to "SET_NEW_DOUBLE_VALUE", "key" to key, "newValue" to it
                    )
                )
            }
        ).show()
    }

    override fun tip(): String {
        return "(Double)"
    }
}


///删除 key
class SpRemoveKeyAction : SpActionBase() {
    override fun action(project: Project, key: String) {
        ///询问弹窗
        val result =
            Messages.showOkCancelDialog(
                "${PluginBundle.get("delete_base_text")} key: $key",
                "${PluginBundle.get("delete_base_text")} SP key",
                PluginBundle.get("delete_base_text"), PluginBundle.get("cancel"),
                Messages.getQuestionIcon()
            )
        if (result == Messages.OK) {
            DioApiService.getInstance().sendByAnyObject(
                mutableMapOf(
                    "action" to "SP_REMOVE_A_KEY", "key" to key
                )
            )
        }

    }

    override fun tip(): String {
        return ""
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text = PluginBundle.get("delete_base_text")
        e.presentation.icon = AllIcons.General.Delete
    }

}


class SPRemoveAllAction : SpActionBase() {
    override fun action(project: Project, key: String) {
        ///询问弹窗
        val result =
            Messages.showOkCancelDialog(
                "${PluginBundle.get("delete_base_text")} SP all keys",
                "${PluginBundle.get("delete_base_text")} SP",
                PluginBundle.get("delete_base_text"), PluginBundle.get("cancel"),
                Messages.getQuestionIcon()
            )
        if (result == Messages.OK) {
            DioApiService.getInstance().sendByAnyObject(
                mutableMapOf(
                    "action" to "SP_REMOVE_ALL_KEY"
                )
            )
        }
    }

    override fun tip(): String {
        return ""
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.text = PluginBundle.get("remove_all_data")
    }
}


private typealias UpdateCurrentValue<T> = (newValue: T) -> Unit

private class UpdateSpValueDialog<T>(
    val key: String,
    val defaultValue: T,
    val type: String,
    val inputBuilder: (panel: Row, value: T, update: UpdateCurrentValue<T>) -> Unit,
    val doOk: (value: T) -> Unit
) : DialogWrapper(true) {

    init {
        super.init()
        title = PluginBundle.get("new_value_in_sp_edit")
    }

    private lateinit var myPanel: DialogPanel
    private var currentValue: T = defaultValue

    override fun createCenterPanel(): JComponent {
        myPanel = panel {
            row("Key") {
                label(key)
            }
            row(PluginBundle.get("new_value_in_title")) {
                inputBuilder(this, defaultValue, updateCurrentValue)
            }
            row {
                comment("Use prefs.set${type}(Key, ${PluginBundle.get("new_value_in_title")});")
            }
        }
        return myPanel
    }

    private val updateCurrentValue: (newValue: T) -> Unit
        get() = {
            currentValue = it
        }

    override fun doOKAction() {
        myPanel.apply()
        doOk(currentValue)
        super.doOKAction()
    }
}

private fun <T : JTextComponent> Cell<T>.bindDouble(prop: MutableProperty<Double>): Cell<T> {
    return bindText(
        { prop.get().toString() },
        { value -> prop.set(getValidatedDoubleValue(value)) })
}

private fun getValidatedDoubleValue(value: String): Double {
    return value.toDouble()
}

fun <T : JTextComponent> Cell<T>.bindDoubleText(getter: () -> Double, setter: (Double) -> Unit): Cell<T> {
    return bindDouble(MutableProperty(getter, setter))
}
