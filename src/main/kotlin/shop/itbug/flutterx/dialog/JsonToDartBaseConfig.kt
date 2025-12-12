package shop.itbug.flutterx.dialog

import com.google.common.base.CaseFormat
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.services.FlutterL10nService
import shop.itbug.flutterx.services.PubspecService
import shop.itbug.flutterx.tools.FreezedClassConfig
import shop.itbug.flutterx.tools.FreezedClassType
import shop.itbug.flutterx.tools.FreezedVersion
import shop.itbug.flutterx.util.VerifyFileDir
import javax.swing.DefaultComboBoxModel
import kotlin.reflect.KMutableProperty0


data class NameRuleConfig(
    val classNameNew: KMutableProperty0<NameFormatRule?>,
    val propertyNameNew: KMutableProperty0<NameFormatRule?>,
    val classNameRaw: KMutableProperty0<NameFormatRule?>,
    val propertyNameRaw: KMutableProperty0<NameFormatRule?>,
)

typealias OnInit = Panel.() -> Unit

///命名规则配置
fun Panel.nameRuleConfig(onChange: NameRuleConfig, init: OnInit? = null): CollapsibleRow {
    return collapsibleGroup(PluginBundle.get("freezed.gen.base.opt"), false) {
        NameStylePanelBuilder.rowBuild(
            this,
            PluginBundle.get("freezed.gen.formatname.classname"),
            "Raw",
            onChange.classNameRaw,
            "To",
            onChange.classNameNew,
        )
        NameStylePanelBuilder.rowBuild(
            this,
            PluginBundle.get("freezed.gen.formatname.properties"),
            "Raw",
            onChange.propertyNameRaw,
            "To",
            onChange.propertyNameNew
        )
        init?.invoke(this)
    }
}


// freezed新版本的设置
data class SaveToDirectoryModelOnChange(
    val directoryGet: () -> String,
    val directorySet: (string: String) -> Unit,
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
                MyRowBuild.folder(this, onChange.directoryGet, onChange.directorySet, project)
            }
            row {
                checkBox(PluginBundle.get("freezed.gen.base.open.in.editor")).bindSelected(onChange.onOpenInEditor)
            }
            otherWidget?.invoke(this)
        }
    }
}

// freeze 新的设置
object FreezedNewSetting {
    fun setting(config: FreezedClassConfig, panel: Panel, project: Project) {
        val defaultVersion = if (PubspecService.getInstance(project).freezedVersionIsThan3()) {
            FreezedVersion.ThreeVersion
        } else {
            FreezedVersion.DefaultVersion
        }
        val box = ComboBox(FreezedVersion.entries.toTypedArray())
        val box2 = ComboBox(FreezedClassType.entries.toTypedArray())
        panel.row("Freezed Setting") {
            cell(box).bindItem({ config.freezedVersion }, {
                config.freezedVersion = it ?: defaultVersion
            })
            cell(box2).bindItem(
                { config.freezedClassType },
                { config.freezedClassType = it ?: FreezedClassType.Sealed })
        }
    }
}


object MyRowBuild {
    /// 2024.3
    fun folder(
        row: Row,
        getter: () -> String,
        setter: (String) -> Unit,
        project: Project
    ): Cell<TextFieldWithBrowseButton> {
        val browse = row.textFieldWithBrowseButton(
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            project,
        ).bindText(getter, setter).align(Align.FILL).addValidationRule(VerifyFileDir.ERROR_MSG) {
            VerifyFileDir.validDirByComponent(it)
        }.validationOnInput {
            if (VerifyFileDir.validDirByComponent(it)) {
                return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
            }
            return@validationOnInput null
        }
        return browse
    }

    /**
     * 配置默认显示的arb file
     */
    fun changeDefaultL10nFile(
        row: Row,
        getter: () -> String,
        setter: (String?) -> Unit,
        project: Project
    ): Cell<ComboBox<@NlsSafe String?>> {
        val arbFiles = FlutterL10nService.getInstance(project).arbFiles
        val comboBox =
            row.comboBox(DefaultComboBoxModel(arbFiles.map { it.file.name }.toTypedArray())).bindItem(getter, setter)
        return comboBox
    }
}

enum class NameFormatRule(val format: CaseFormat, val eg: String) {
    LOWER_HYPHEN(
        CaseFormat.LOWER_HYPHEN,
        "Hyphenated variable naming convention, e.g., \"lower-hyphen\""
    ),
    LOWER_UNDERSCORE(
        CaseFormat.LOWER_UNDERSCORE,
        "C++ variable naming convention, e.g., \"lower_underscore\"."
    ),
    LOWER_CAMEL(CaseFormat.LOWER_CAMEL, "Java variable naming convention, e.g., \"lowerCamel\"."), UPPER_CAMEL(
        CaseFormat.UPPER_CAMEL,
        "Java and C++ class naming convention, e.g., \"UpperCamel\"."
    ),
    UPPER_UNDERSCORE(
        CaseFormat.UPPER_UNDERSCORE, "Java and C++ constant naming convention, e.g., \"UPPER_UNDERSCORE\"."
    );

    override fun toString(): String {
        return format.name
    }
}

object NameStylePanelBuilder {

    /**
     * 命名设置
     */
    fun rowBuild(
        panel: Panel,
        label: String,
        leftLabel: String,
        leftBind: KMutableProperty0<NameFormatRule?>,
        rightLabel: String,
        rightBind: KMutableProperty0<NameFormatRule?>
    ) {
        val box = ComboBox(NameFormatRule.entries.toTypedArray())
        val box2 = ComboBox(NameFormatRule.entries.toTypedArray())
        fun htmlBuild(item: NameFormatRule): String {
            return HtmlChunk.div().children(
                HtmlChunk.text(item.name).bold(),
                HtmlChunk.nbsp(2),
                HtmlChunk.text(item.eg)
            ).toString()
        }
        panel.row(label) {
            cell(box).bindItem(leftBind).label(leftLabel)
            cell(box2).bindItem(rightBind).label(rightLabel)
        }.contextHelp(
            HtmlChunk.html().addRaw(NameFormatRule.entries.joinToString(HtmlChunk.br().toString()) { htmlBuild(it) })
                .toString()
        )
    }
}
