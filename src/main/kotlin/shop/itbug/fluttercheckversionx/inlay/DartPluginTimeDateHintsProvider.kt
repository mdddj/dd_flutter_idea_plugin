package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.util.Util
import shop.itbug.fluttercheckversionx.util.getPluginName
import shop.itbug.fluttercheckversionx.util.isDartPluginElement
import javax.swing.JComponent

const val pluginInfoSettingKey = "dart plugin infos show"
const val pluginInfoName = "dart plugin info name"
class DartPluginTimeDateHintsProvider:InlayHintsProvider<TimeDateHintSetting> {

    override val key: SettingsKey<TimeDateHintSetting> get() = SettingsKey(pluginInfoSettingKey)

    override val name: String get() = pluginInfoName

    override val previewText: String get() = """
        dependencies:
            dio: ^3.0.6
    """.trimIndent()

    override fun createSettings(): TimeDateHintSetting {
        return TimeDateHintSetting(showLastUpdateTime = true, adsShow = true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: TimeDateHintSetting,
        sink: InlayHintsSink
    ): InlayHintsCollector? {

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if(element.isDartPluginElement()){
//                    val callRespose = ServiceCreate.create(PubService::class.java).callPluginDetails(element.getPluginName())
//                    val body = callRespose.execute().body()
//                    if(body!=null){
//                        val myFactory = HintsInlayPresentationFactory(factory = factory)
//                        println("获取到插件信息:$body")
//                        sink.addInlineElement(element.endOffset,false,myFactory.simpleText(formatText(body), "提示"),true)
//
//                    }else{
//                        println("body为null")
//                    }
                }
                return true
            }

        }
    }

    fun formatText(model: PubVersionDataModel) : String{
        val sb = StringBuilder()
        val timer = model.latest.published
       val v =  Util.Companion.RelativeDateFormat.format(timer)
                sb.append("最后更新时间:$v")
        return sb.toString()
    }

    override fun createConfigurable(settings: TimeDateHintSetting): ImmediateConfigurable {
        return PluginInfosShowSettingPanel()
    }
}


/**
 * 配置面板
 */
class PluginInfosShowSettingPanel :ImmediateConfigurable{

    private val timeShowCheckBox = JBCheckBox()
    private val adsShowCheckBox = JBCheckBox()
    override fun createComponent(listener: ChangeListener): JComponent {
        return FormBuilder.createFormBuilder().addLabeledComponent("展示插件最后更新时间", timeShowCheckBox)
              .addLabeledComponent("展示广告", adsShowCheckBox).panel
    }


}


/**
 * 设置
 * @param showLastUpdateTime 是否展示插件最后更新的时间
 */
data class TimeDateHintSetting(val showLastUpdateTime:Boolean,val adsShow: Boolean)