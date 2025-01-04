package shop.itbug.fluttercheckversionx.dialog

import com.google.common.base.CaseFormat
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.VerifyFileDir
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
                MyRowBuild.folder(this, onChange.onDirectoryChange, project)
            }
            row {
                checkBox(PluginBundle.get("freezed.gen.base.open.in.editor")).bindSelected(onChange.onOpenInEditor)
            }
            otherWidget?.invoke(this)
        }
    }

}


private object MyRowBuild {
    /// 2024.3
//    fun folder(row: Row, onChange: KMutableProperty0<String>, project: Project) {
//        row.textFieldWithBrowseButton(
//            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
//            project,
//        ).bindText(onChange).align(Align.FILL).addValidationRule(VerifyFileDir.ERROR_MSG) {
//            VerifyFileDir.validDirByComponent(it)
//        }.validationOnInput {
//            if (VerifyFileDir.validDirByComponent(it)) {
//                return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
//            }
//            return@validationOnInput null
//        }
//    }

    /// 2023.2
    fun folder(row: Row, onChange: KMutableProperty0<String>, project: Project) {
        row.textFieldWithBrowseButton(
            PluginBundle.get("select_a_folder"),
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            { it.path },
        ).bindText(onChange).align(Align.FILL).addValidationRule(VerifyFileDir.ERROR_MSG) {
            VerifyFileDir.validDirByComponent(it)
        }.validationOnInput {
            if (VerifyFileDir.validDirByComponent(it)) {
                return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
            }
            return@validationOnInput null
        }
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
        val box = ComboBox<NameFormatRule>(NameFormatRule.entries.toTypedArray())
        val box2 = ComboBox<NameFormatRule>(NameFormatRule.entries.toTypedArray())
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
