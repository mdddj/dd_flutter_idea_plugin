package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.MyDartPackage
import shop.itbug.fluttercheckversionx.services.noused.DartNoUsedCheckResultModel
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList

/**
 * @Author: 梁典典
 * @Date: 2024/09/21/上午11:18
 * @Description:
 */
class DartNoUsedResultModel(val project: Project, val result: DartNoUsedCheckResultModel) :
    DialogWrapper(project, true) {
    private val list = JBList<MyDartPackage>().apply {
        this.model = DefaultListModel<MyDartPackage>().apply { addAll(result.noUsedPackageList) }
        this.cellRenderer = object : ColoredListCellRenderer<MyDartPackage>() {
            override fun customizeCellRenderer(
                p0: JList<out MyDartPackage>,
                p1: MyDartPackage?,
                p2: Int,
                p3: Boolean,
                p4: Boolean
            ) {
                icon = MyIcons.flutter
                append(p1?.packageName ?: "-")
            }

        }
    }

    init {
        super.init()
        title = PluginBundle.get("check_un_used_all_import_size_result") + "- FlutterX"
    }

    override fun createCenterPanel(): JComponent {
        return panel {

            row(PluginBundle.get("check_un_used_all_locals")) {
                label("${result.localPackageSize}")
            }
            row(PluginBundle.get("check_un_used_all_import_size")) {
                label("${result.importAllSize}")
            }
            row(PluginBundle.get("check_un_used_all_import_size_other")) {
                label("${result.packageImportSize}")
            }
            row {
                scrollCell(list).comment(PluginBundle.get("check_un_used_all_import_size_tips"))
            }
        }
    }
}