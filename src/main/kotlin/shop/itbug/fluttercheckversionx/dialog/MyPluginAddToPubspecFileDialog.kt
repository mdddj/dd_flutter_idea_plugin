package shop.itbug.fluttercheckversionx.dialog

import cn.hutool.db.Entity
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import javax.swing.JComponent

/**
 * 添加插件到pubspec.yaml
 */
class MyPluginAddToPubspecFileDialog(override val project: Project, entity: Entity) : MyDialogWrapper(project) {


    private val pluginName: String = entity.getStr("name")
    var pType = FlutterPluginType.values().first()

    init {
        super.init()
        title = "Add a dependency to the pubspec.yaml file"
    }


    override fun createCenterPanel(): JComponent {
        return panel {

            row("Name ") {
                label(pluginName).component.apply {
                    font = JBFont.h4()
                }
            }
            row("Type") {
                segmentedButton(FlutterPluginType.values().toList()) {
                    it.title
                }.bind(object : ObservableMutableProperty<FlutterPluginType> {
                    override fun set(value: FlutterPluginType) {
                        pType = value
                    }

                    override fun afterChange(listener: (FlutterPluginType) -> Unit) {
                    }

                    override fun get(): FlutterPluginType {
                        return pType
                    }

                })
            }
        }
    }


    override fun doOKAction() {
        super.doOKAction()
        MyPsiElementUtil.insertPluginToPubspecFile(project,pluginName,"any",pType)
    }
}