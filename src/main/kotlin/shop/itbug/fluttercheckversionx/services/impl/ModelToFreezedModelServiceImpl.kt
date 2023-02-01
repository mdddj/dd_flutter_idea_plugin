package shop.itbug.fluttercheckversionx.services.impl

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.model.DartClassProperty
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.ModelToFreezedModelService
import shop.itbug.fluttercheckversionx.util.*

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


    override fun jsonObjectToFreezedCovertModelList(
        jsonObject: JSONObject,
        oldList: MutableList<FreezedCovertModel>,
        className: String
    ): MutableList<FreezedCovertModel> {
        val filter = oldList.none { it.className == className }
        if (filter) {
            val rootModel = jsonObjectToFreezedCovertModel(jsonObject, className)
            oldList.add(rootModel)
            jsonObject.forEach { key, value ->
                if (value is JSONObject) {
                    jsonObjectToFreezedCovertModelList(value, oldList, key.toString().formatDartName())
                } else if (value is JSONArray && value.isNotEmpty() && value.first() is JSONObject) {
                    val parse = JSONObject.parse(JSONObject.toJSONString(value.findPropertiesMaxLenObject()))
                    jsonObjectToFreezedCovertModelList(parse, oldList, key.toString().formatDartName())
                }
            }
        }
        return oldList
    }

    //
    override fun jsonObjectToFreezedCovertModel(jsonObject: JSONObject, className: String): FreezedCovertModel {
        val properties = mutableListOf<DartClassProperty>()
        jsonObject.forEach { key, value ->
            val dartType = DartJavaCovertUtil.getDartType(value, key)
            val dartClassProperty = DartClassProperty(
                type = dartType,
                name = key,
                isNonNull = false,
                finalPropertyValue = value,
                finalPropertyName = key
            )
            properties.add(dartClassProperty)
        }
        return FreezedCovertModel(properties = properties, className = className)
    }


}