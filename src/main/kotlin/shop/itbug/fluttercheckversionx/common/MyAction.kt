package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier
import javax.swing.Icon

abstract class MyAction : AnAction {

    constructor()
    constructor(name: Supplier<String>) : super(name)
    constructor(icon: Icon) : super(icon)

    constructor(name: Supplier<String>, icon: Icon) : super(name, icon)
    constructor(title: String, desc: String, icon: Icon) : super(title, desc, icon)

}

abstract class MyDumbAwareAction : DumbAwareAction {
    constructor()

    constructor(name: Supplier<String>) : super(name)
    constructor(icon: Icon) : super(icon)

    constructor(title: String, desc: String, icon: Icon) : super(title, desc, icon)

}

abstract class MyToggleAction : ToggleAction {

    constructor()
    constructor(name: Supplier<String>) : super(name)

    constructor(name: String, desc: String, icon: Icon) : super(name, desc, icon)

}

abstract class MyComboBoxAction : ComboBoxAction() {

}