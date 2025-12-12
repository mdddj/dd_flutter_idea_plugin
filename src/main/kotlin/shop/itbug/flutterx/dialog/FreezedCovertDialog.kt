package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.flutterx.model.FreezedCovertModel
import shop.itbug.flutterx.model.getPropertiesString
import shop.itbug.flutterx.util.MyDartPsiElementUtil
import shop.itbug.flutterx.widget.DartEditorTextPanel
import javax.swing.JComponent

class FreezedCovertDialog(val project: Project, val model: FreezedCovertModel) : DialogWrapper(project) {

    data class GenProperties(
        var genFromJson: Boolean = false,
        var genPartOf: Boolean = false,
        var funsToExt: Boolean = false,
        var className: String = ""
    )

    val setting = GenProperties().apply {
        className = model.className
    }


    private var editView = DartEditorTextPanel(project)


    init {
        super.init()
        title = "To freezed class (${model.className})"
        generateFreezedModel()
    }

    override fun createCenterPanel(): JComponent {

        return object : BorderLayoutPanel() {
            init {
                addToCenter(editView)
            }
        }
    }

    private fun generateFreezedModel() {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, model.className, model.getPropertiesString())
        editView.text = genFreezedClass.text
    }


}