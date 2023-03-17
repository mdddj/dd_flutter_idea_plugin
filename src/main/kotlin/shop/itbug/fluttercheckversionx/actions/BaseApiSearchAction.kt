package shop.itbug.fluttercheckversionx.actions

import com.intellij.find.findUsages.FindUsagesHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.search.GlobalSearchScope
import shop.itbug.fluttercheckversionx.common.MyAction

class BaseApiSearchAction : MyAction() {
    override fun actionPerformed(e: AnActionEvent) {


        val element = e.getData(CommonDataKeys.PSI_ELEMENT)!!
        val project = e.project!!

        FindUsagesHelper.processUsagesInText(element, listOf("HttpApi"),true, GlobalSearchScope.allScope(project)) {
            println("进来了: ${it.element?.text}")
            true
        }


    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.PSI_ELEMENT) != null && e.project != null
        super.update(e)
    }
}