package shop.itbug.fluttercheckversionx.widget

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Unmodifiable
import javax.swing.JComponent

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
                if(reGetActions){
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
                ActivityTracker.getInstance().inc()
                super.update(e)
            }
        }
    }


    /**
     * 枚举类型的 action
     */
    abstract class EnumPolicySettingAction<T : Enum<T>>(val policies: Array<T>) : ComboBoxSettingAction<T>() {


        override fun update(e: AnActionEvent) {
            super.update(e)
            e.presentation.isEnabledAndVisible = policies.size > 1
        }


        override val reGetActions: Boolean
            get() = false


        override val availableOptions: MutableList<T>
            get() = ContainerUtil.sorted(listOf(*policies))

        fun getValue(): T  {
            val value = storedValue
            if (ArrayUtil.contains(value, *policies)) return value

            val substitutes = getValueSubstitutes(value)
            for (substitute in substitutes) {
                if (ArrayUtil.contains(substitute, *policies)) return substitute
            }

            return policies[0]
        }



        protected abstract val storedValue: T

        protected fun getValueSubstitutes(value: T): List<T> {
            return emptyList()
        }
    }



}