package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

abstract class MyDialogWrapper(open val project: Project): DialogWrapper(project) {

    constructor(project: Project,title:String):this(project){
        super.setTitle(title)
    }


}