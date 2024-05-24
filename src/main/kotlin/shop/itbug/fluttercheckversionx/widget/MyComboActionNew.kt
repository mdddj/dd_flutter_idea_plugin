package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.ui.SearchTextField
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Unmodifiable
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MyComboActionNew {

    //
    // 下拉类型的 action
    //
    abstract class ComboBoxSettingAction<T> : ComboBoxAction(), DumbAware {
        private var myActions: DefaultActionGroup? = null

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }

        override fun update(e: AnActionEvent) {
            val presentation = e.presentation
            presentation.text = getText(value)
        }

        override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
            return actions
        }

        ///重新获取actions
        protected abstract val reGetActions: Boolean

        val actions: DefaultActionGroup
            get() {
                if (reGetActions) {
                    myActions = null
                }
                if (myActions == null) {
                    myActions = DefaultActionGroup()
                    for (setting in availableOptions) {
                        myActions!!.add(MyAction(setting))
                    }
                }
                return myActions!!
            }

        protected abstract val availableOptions: @Unmodifiable MutableList<T>

        protected abstract var value: T

        open fun setNewValue(value: T, e: AnActionEvent) {}

        @Nls
        protected abstract fun getText(option: T): String

        private inner class MyAction(private val myOption: T) : AnAction(getText(myOption)), Toggleable, DumbAware {
            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.EDT
            }

            override fun update(e: AnActionEvent) {
                Toggleable.setSelected(e.presentation, value === myOption)
                super.update(e)
            }

            override fun actionPerformed(e: AnActionEvent) {
                value = myOption
                setNewValue(myOption, e)
            }
        }
    }


    ///开关样式的操作
    abstract class ToggleActionGroup<T>(private val values: Array<T>) : ActionGroup(), DumbAware {


        abstract fun getText(value: T): String

        abstract var value: T


        override fun getChildren(e: AnActionEvent?): Array<AnAction> {
            val actions = ArrayList<MyAction>()
            actions.addAll(values.map { MyAction(it) })
            return actions.toTypedArray()
        }


        private inner class MyAction(val item: T) : DumbAwareToggleAction({ getText(item) }) {
            override fun isSelected(e: AnActionEvent): Boolean {
                return value == item
            }

            override fun setSelected(e: AnActionEvent, state: Boolean) {
                value = item
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT


            override fun update(e: AnActionEvent) {
                Toggleable.setSelected(e.presentation, isSelected(e))
                super.update(e)
            }

        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }


    ///自定义搜索的 action
    abstract class MySearchAnAction : AnAction(), CustomComponentAction, DocumentListener {
        private val searchTextField = SearchTextField()


        override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
            searchTextField.addDocumentListener(this)
            return searchTextField
        }

        override fun changedUpdate(e: DocumentEvent?) {
            e?.let {
                val text = e.document.getText(0, e.document.length)
                changeText(text, e)
            }
        }

        abstract fun changeText(text: String, e: DocumentEvent)

        override fun insertUpdate(e: DocumentEvent?) {
            changedUpdate(e)
        }

        override fun removeUpdate(e: DocumentEvent?) {
            changedUpdate(e)
        }

        override fun actionPerformed(e: AnActionEvent) {
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }

    }

}