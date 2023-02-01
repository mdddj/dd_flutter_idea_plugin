package shop.itbug.fluttercheckversionx.services

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel

interface ModelToFreezedModelService {
    fun psiElementToFreezedCovertModel(classPsiElement: DartClassDefinitionImpl) : FreezedCovertModel
    fun anActionEventToFreezedCovertModel(event: AnActionEvent) : FreezedCovertModel
    fun jsonObjectToFreezedCovertModelList(jsonObject: JSONObject,oldList: MutableList<FreezedCovertModel> = mutableListOf()) : List<FreezedCovertModel>

    fun jsonObjectToFreezedCovertModel(jsonObject: JSONObject) : FreezedCovertModel
}