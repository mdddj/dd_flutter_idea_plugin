package shop.itbug.fluttercheckversionx.services.impl

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.model.DartClassProperty
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.ModelToFreezedModelService
import shop.itbug.fluttercheckversionx.util.DartJavaCovertUtil
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.getDartClassDefinition

class ModelToFreezedModelServiceImpl : ModelToFreezedModelService {
    override fun psiElementToFreezedCovertModel(classPsiElement: DartClassDefinitionImpl): FreezedCovertModel {
        val classProperties = DartPsiElementUtil.getClassProperties(classPsiElement)
        val models = DartPsiElementUtil.getModels(classProperties)
        return FreezedCovertModel(properties = models, className = classPsiElement.componentName.text)
    }

    override fun anActionEventToFreezedCovertModel(event: AnActionEvent): FreezedCovertModel {
        val dartClassDefinition = event.getDartClassDefinition()!!
        return psiElementToFreezedCovertModel(dartClassDefinition)
    }

    override fun jsonObjectToFreezedCovertModelList(jsonObject: JSONObject,oldList: MutableList<FreezedCovertModel>): List<FreezedCovertModel> {
        val rootModel = jsonObjectToFreezedCovertModel(jsonObject)
        oldList.add(rootModel)
        jsonObject.values.forEach {
            if(it is JSONObject) {
                val jsonObjectToFreezedCovertModelList = jsonObjectToFreezedCovertModelList(it, oldList)
                oldList.addAll(jsonObjectToFreezedCovertModelList)
            }else if(it is JSONArray) {
                if(it.isNotEmpty()) {
                    val parse = JSONObject.parse(JSONObject.toJSONString(it.first()))
                    val jsonObjectToFreezedCovertModelList = jsonObjectToFreezedCovertModelList(parse, oldList)
                    oldList.addAll(jsonObjectToFreezedCovertModelList)
                }
            }
        }
        return oldList
    }

    override fun jsonObjectToFreezedCovertModel(jsonObject: JSONObject): FreezedCovertModel {
        val properties = mutableListOf<DartClassProperty>()
        jsonObject.forEach { key, value ->
            val dartType = DartJavaCovertUtil.getDartType(value::class.java, key)
            val dartClassProperty = DartClassProperty(type = dartType, name = key, isNonNull = false)
            properties.add(dartClassProperty)
        }
        return FreezedCovertModel(properties = properties, className = "FreezedModel")
    }


}