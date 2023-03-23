package shop.itbug.fluttercheckversionx.common

import cn.hutool.core.swing.ScreenUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

abstract class MyDialogWrapper(open val project: Project): DialogWrapper(project) {

    constructor(project: Project,title:String):this(project){
        super.setTitle(title)
    }


    fun setBaseSize() {
        setSize(width - 200, height-200)
        setLocation(100,100)
    }


    val width: Int get() = ScreenUtil.getWidth()
    val height: Int get() = ScreenUtil.getHeight()
}