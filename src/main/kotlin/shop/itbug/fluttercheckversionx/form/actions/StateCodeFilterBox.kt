package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.openapi.actionSystem.ex.ComboBoxAction


/**
 * 筛选状态码
 */
class StateCodeFilterBox() : ComboBoxAction()  {

    private  var methodTypes = listOf("All","Get","Post","Delete","Put")

}
