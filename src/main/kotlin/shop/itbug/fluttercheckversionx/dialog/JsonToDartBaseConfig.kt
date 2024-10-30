package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.tools.NameFormat
import shop.itbug.fluttercheckversionx.util.VerifyFileDir
import kotlin.reflect.KMutableProperty0


data class NameRuleConfig(
    val className: KMutableProperty0<NameFormat>,
    val propertiesName: KMutableProperty0<NameFormat>,
)

typealias OnInit = Panel.() -> Unit

///命名规则配置
fun Panel.nameRuleConfig(onChange: NameRuleConfig, init: OnInit? = null): CollapsibleRow {
    return collapsibleGroup(PluginBundle.get("freezed.gen.base.opt"), false) {
        buttonsGroup(PluginBundle.get("freezed.gen.formatname.classname") + ":") {
            row {
                NameFormat.entries.forEach {
                    radioButton(it.title, it)
                    contextHelp(it.example, PluginBundle.get("freezed.gen.formatname.example"))
                }
            }
        }.bind(onChange.className)
        buttonsGroup(PluginBundle.get("freezed.gen.formatname.properties") + ":") {
            row {
                NameFormat.entries.forEach {
                    radioButton(it.title, it)
                    contextHelp(it.example, PluginBundle.get("freezed.gen.formatname.example"))
                }
            }
        }.bind(onChange.propertiesName)
        init?.invoke(this)
    }
}


data class SaveToDirectoryModelOnChange(
    val onDirectoryChange: KMutableProperty0<String>,
    val onFilenameChange: KMutableProperty0<String>,
    val onOpenInEditor: KMutableProperty0<Boolean>,
)


/// 保存文件到目录
fun Row.saveToDirectoryConfig(
    project: Project, onChange: SaveToDirectoryModelOnChange, otherWidget: OnInit? = null
): Panel {
    return panel {
        group(PluginBundle.get("save.to.directory")) {
            row(PluginBundle.get("g.2")) {
                textField().align(Align.FILL).bindText(onChange.onFilenameChange)
                    .addValidationRule(VerifyFileDir.ENTER_YOU_FILE_NAME) {
                        it.text.trim().isBlank()
                    }.validationOnInput {
                        if (it.text.trim().isBlank()) {
                            return@validationOnInput error(VerifyFileDir.ENTER_YOU_FILE_NAME)
                        }
                        return@validationOnInput null
                    }
            }
            row(PluginBundle.get("g.3")) {
                textFieldWithBrowseButton(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project) {
                    it.path
                }.bindText(onChange.onDirectoryChange).align(Align.FILL).addValidationRule(VerifyFileDir.ERROR_MSG) {
                    VerifyFileDir.validDirByComponent(it)
                }.validationOnInput {
                    if (VerifyFileDir.validDirByComponent(it)) {
                        return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
                    }
                    return@validationOnInput null
                }
//                textFieldWithBrowseButton(
//                    "Select Dir", project, FileChooserDescriptorFactory.createSingleFolderDescriptor()
//                ) { it.path }.bindText(onChange.onDirectoryChange).align(Align.FILL)
//                    .addValidationRule(VerifyFileDir.ERROR_MSG) {
//                        VerifyFileDir.validDirByComponent(it)
//                    }.validationOnInput {
//                        if (VerifyFileDir.validDirByComponent(it)) {
//                            return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
//                        }
//                        return@validationOnInput null
//                    }
            }
            row {
                checkBox(PluginBundle.get("freezed.gen.base.open.in.editor")).bindSelected(onChange.onOpenInEditor)
            }
            otherWidget?.invoke(this)
        }
    }

}
