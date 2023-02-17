package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton

class MyActionButton(
    action: AnAction, presentationText: String, placeText: String
) : ActionButton(action, Presentation(presentationText), placeText, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {

    constructor(iconAnAction: AnAction) : this(iconAnAction,"","")

}