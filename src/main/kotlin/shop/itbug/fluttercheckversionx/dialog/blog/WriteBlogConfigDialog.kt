package shop.itbug.fluttercheckversionx.dialog.blog

import com.alibaba.fastjson2.toJSONString
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import org.apache.xerces.impl.dv.ValidatedInfo
import retrofit2.Call
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.common.MySelecter
import shop.itbug.fluttercheckversionx.model.BlogCategory
import shop.itbug.fluttercheckversionx.model.BlogWriteModel
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.JComponent
/**
 * 显示发布博客窗口
 */
fun Project.showWriteBlogDialog(e: AnActionEvent) {
    WriteBlogConfigDialog(this,e).show()
}


///发布博客的弹出
class WriteBlogConfigDialog(project: Project,val e: AnActionEvent) : MyDialogWrapper(project) {


    private val blogModel = BlogWriteModel()

    private lateinit var ui : DialogPanel
 
    
    ///选择博客分类
    private val categoryWidget = object: MySelecter<BlogCategory>() {
        override fun getData(): Call<JSONResult<List<BlogCategory>>> {
            return  SERVICE.create<ItbugService>().findAllBlogCategory()
        }

        override fun getCompeont(value: BlogCategory, index: Int, isSelected: Boolean): JComponent {
            return JBLabel(value.name)
        }
        
        

    }
    
    
    init {
        super.init()
        title = "发布博客"
    }

    override fun createCenterPanel(): JComponent {
        ui = panel {
            row ("标题") {
                textField().bindText(blogModel::title).validationInfo { if(it.text.isEmpty()) ValidationInfoBuilder(it).error("标题不能为空") else null }
            }

            row("访问别名") {
                textField().bindText(blogModel::alias)
            }

            row ("标签"){
                textField().bindText({blogModel.tags.joinToString { "," }},{blogModel.tags = it.split(",")})
            }
            
            row ("分类") {
                cell(categoryWidget).validationInfo { if(categoryWidget.isSelectionEmpty) ValidationInfoBuilder(categoryWidget).error("请选择分类") else null }
            }
        }
        return ui
    }


    // 
    override fun doOKAction() {
        val content = getContent()
        if(content == null) {
            project.toastWithError("无法获取正文内容")
        }
       content?.apply {
            blogModel.content = this
        }
        ui.apply()
        println(blogModel.toJSONString())
        super.doOKAction()
    }


    
    private fun getContent () : String? {
        return e.getData(CommonDataKeys.EDITOR)?.document?.text
    }
}