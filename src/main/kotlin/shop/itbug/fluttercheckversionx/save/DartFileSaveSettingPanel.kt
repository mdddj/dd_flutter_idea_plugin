package shop.itbug.fluttercheckversionx.save

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.util.Alarm
import com.intellij.util.lateinitVal
import javax.swing.SwingUtilities


data class DartFileSaveSettingModel(
    var enable: Boolean = false,
    var command: String = "",
    var runType: Boolean = false,
    var title: String = ""
)

@Service
@State(name = "DartFileSaveSettingState", storages = [Storage("DartFileSaveSettingState.xml")])
class DartFileSaveSettingState private constructor() : PersistentStateComponent<DartFileSaveSettingModel> {
    private var setting = DartFileSaveSettingModel()
    override fun getState(): DartFileSaveSettingModel {
        return setting
    }

    override fun loadState(state: DartFileSaveSettingModel) {
        this.setting = state
    }


    companion object {

        //获取实例
        fun getInstance(): DartFileSaveSettingState {
            return service()
        }
    }

}


fun dartFileSaveSettingPanel(
    disposable: Disposable,
    state: DartFileSaveSettingModel,
    valueChanged: (v: Boolean) -> Unit
): DialogPanel {

    var p by lateinitVal<DialogPanel>()

    p = panel {
        row {
            checkBox("是否启用此功能").bindSelected(state::enable)
        }
        row {
            checkBox("执行方式: 默认静默无感执行,会在后台开启一个线程执行,勾选后会打开Terminal执行").bindSelected(state::runType)
        }
        row {
            textField().bindText(state::title).comment("自定义后台任务名称")
        }
        row {
            textArea().bindText(state::command).rows(3)
        }.comment("执行命令 (提示: 使用{path},在执行的时候会替换成保存的文件路径)")


    }


    val alarm = Alarm(disposable)


    fun initValidation() {
        alarm.addRequest({
            valueChanged.invoke(p.isModified())
            initValidation()
        }, 1000)
    }

    SwingUtilities.invokeLater {
        initValidation()
    }

    val newD = Disposer.newDisposable()
    p.registerValidators(newD)
    Disposer.register(newD, disposable)

    return p
}
