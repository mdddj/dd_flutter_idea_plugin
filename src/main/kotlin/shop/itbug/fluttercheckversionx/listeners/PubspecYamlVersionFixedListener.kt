package shop.itbug.fluttercheckversionx.listeners

import com.intellij.analysis.problemsView.Problem
import com.intellij.analysis.problemsView.ProblemsListener

class PubspecYamlVersionFixedListener : ProblemsListener {

    override fun problemAppeared(problem: Problem) {
        println("problemAppeared 显示 -- $problem")
    }

    override fun problemDisappeared(problem: Problem) {
        println("problemDisappeared 消失 -- $problem")
    }

    override fun problemUpdated(problem: Problem) {
        println("problemUpdated 修改 -- $problem")
    }


}