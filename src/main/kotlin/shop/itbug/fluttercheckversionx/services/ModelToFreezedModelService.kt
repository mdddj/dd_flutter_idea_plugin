package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import kotlinx.serialization.json.JsonElement
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel

const val DEFAULT_CLASS_NAME = "Root"

interface ModelToFreezedModelService {
    fun psiElementToFreezedCovertModel(classPsiElement: DartClassDefinitionImpl): FreezedCovertModel
    fun anActionEventToFreezedCovertModel(event: AnActionEvent): FreezedCovertModel
    fun jsonObjectToFreezedCovertModelList(
        jsonObject: JsonElement,
        oldList: MutableList<FreezedCovertModel> = mutableListOf(),
        className: String = DEFAULT_CLASS_NAME
    ): MutableList<FreezedCovertModel>

    fun jsonObjectToFreezedCovertModel(
        jsonObject: JsonElement,
        className: String = DEFAULT_CLASS_NAME
    ): FreezedCovertModel

}