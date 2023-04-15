package shop.itbug.fluttercheckversionx.dialog.jobs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import org.intellij.plugins.markdown.ui.preview.MarkdownPreviewFileEditor
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.params.AddJobParams
import shop.itbug.fluttercheckversionx.util.SwingUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.jobs.JobsCitySelectWidgetWithComBox
import javax.swing.JComponent


class AddJobsDialog(val project: Project) : DialogWrapper(project) {

    private val mkComp = SwingUtil.getMkEditor(project,"# 请输入招聘内容")
    private val citySelect = JobsCitySelectWidgetWithComBox(project)

    private var title: String = ""
    private var tag: String = ""

    private lateinit var titleTextField: Cell<JBTextField>

    init {
        super.init()
        title = "发布新职位"
        setSize(800, 600)
        (mkComp.previewEditor as MarkdownPreviewFileEditor).selectNotify()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("标题") {
                titleTextField = textField().bindText({ title }, { title = it }).validationOnInput {
                    if (it.text.length <= 10) {
                        ValidationInfoBuilder(titleTextField.component).error("标题不能少于10字")
                    }
                    null
                }
            }
            row("城市") {
                cell(citySelect)
                textField().bindText({ tag }, { tag = it }).label("月薪", LabelPosition.LEFT)
            }
            row {
                cell(mkComp.component)
            }
        }
    }

    private val contentText: String get() = mkComp.editor.document.text


    override fun doOKAction() {
        val r = SERVICE.create<ItbugService>().addJob(
            AddJobParams(
                cateId = citySelect.selectResourceCategory!!.id.toLong(),
                content = contentText,
                title = title
            )
        ).execute()
        if (r.isSuccessful) {
            println(r)
            if (r.body()?.state == 200) {
                project.toast("发布成功")
            } else {
                project.toastWithError(r.body()?.message ?: "发布失败")
            }
        } else {
            project.toastWithError("发布失败: ${r.code()}")
        }
        super.doOKAction()
    }

    override fun doValidate(): ValidationInfo? {

        if (citySelect.selectResourceCategory == null) {
            return ValidationInfo("内容不能少于10字", citySelect)
        }
        if (contentText.length <= 40) {
            return ValidationInfo("内容不能少于40字", mkComp.component)
        }
        return super.doValidate()
    }
}