package shop.itbug.fluttercheckversionx.temp

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

///riverpod stf
class ConsumerfulTemp: TemplateContextType("FLUTTER","Flutter") {

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        return templateActionContext.file.name.endsWith(".dart")
    }
}