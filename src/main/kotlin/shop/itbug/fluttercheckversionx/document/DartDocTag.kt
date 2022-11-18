package shop.itbug.fluttercheckversionx.document

import com.intellij.openapi.project.Project

/**
 * markdown 渲染组件所需要的参数
 */
class MyMarkdownDocRenderObject(val text: String,val project: Project) {

    fun getContent() : String {
        return text
    }



}