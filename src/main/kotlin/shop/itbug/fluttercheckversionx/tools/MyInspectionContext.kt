package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.GlobalInspectionContext
import com.intellij.codeInspection.ex.InspectionToolWrapper
import com.intellij.codeInspection.ex.Tools
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension
import com.intellij.openapi.util.Key

class MyInspectionContext : GlobalInspectionContextExtension<MyInspectionContext> {
    override fun getID(): Key<MyInspectionContext> {
        return Key.create("MyInspectionContext")
    }

    override fun performPreRunActivities(
        globalTools: MutableList<Tools>,
        localTools: MutableList<Tools>,
        context: GlobalInspectionContext
    ) {

        print("performPreRunActivities -- 执行了");
    }

    override fun performPostRunActivities(
        inspections: MutableList<InspectionToolWrapper<*, *>>,
        context: GlobalInspectionContext
    ) {

        println("performPostRunActivities==-")
    }

    override fun cleanup() {
        println("cleanup 执行了");
    }

}