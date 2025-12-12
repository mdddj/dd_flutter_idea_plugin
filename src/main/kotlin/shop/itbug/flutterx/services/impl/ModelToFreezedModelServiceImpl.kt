package shop.itbug.flutterx.services.impl

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import shop.itbug.flutterx.model.DartClassProperty
import shop.itbug.flutterx.model.FreezedCovertModel
import shop.itbug.flutterx.services.ModelToFreezedModelService
import shop.itbug.flutterx.util.*

class ModelToFreezedModelServiceImpl : ModelToFreezedModelService {
    override fun psiElementToFreezedCovertModel(classPsiElement: DartClassDefinitionImpl): FreezedCovertModel {
        val classProperties = DartPsiElementUtil.getClassProperties(classPsiElement)
        val models = DartPsiElementUtil.getModels(classProperties)
        return FreezedCovertModel(
            properties = models, className = classPsiElement.componentName.text, isDartClassElementType = true
        )
    }

    override fun anActionEventToFreezedCovertModel(event: AnActionEvent): FreezedCovertModel {
        val dartClassDefinition = event.getDartClassDefinition()!!
        return psiElementToFreezedCovertModel(dartClassDefinition)
    }


    override fun jsonObjectToFreezedCovertModelList(
        jsonObject: JsonElement, oldList: MutableList<FreezedCovertModel>, className: String
    ): MutableList<FreezedCovertModel> {
        val filter = oldList.none { it.className == className }
        if (filter) {
            val rootModel = jsonObjectToFreezedCovertModel(jsonObject, className)
            oldList.add(rootModel)

            if (jsonObject.isObject) {
                jsonObject.jsonObject.forEach { k, v ->
                    run {
                        if (v.isObject) {
                            jsonObjectToFreezedCovertModelList(v, oldList, k.formatDartName())
                        }
                        if (v.isJsonArray) {
                            val maxEle = v.jsonArray.findPropertiesMaxLenObject()
                            jsonObjectToFreezedCovertModelList(maxEle, oldList, k.formatDartName())
                        }
                    }
                }
            }


        }
        return oldList
    }

    override fun jsonObjectToFreezedCovertModel(jsonObject: JsonElement, className: String): FreezedCovertModel {
        val properties = mutableListOf<DartClassProperty>()
        if (jsonObject.isObject) {
            jsonObject.jsonObject.forEach { key, value ->
                val dartType = DartJavaCovertUtil.getDartType(value, key)
                val dartClassProperty = DartClassProperty(
                    type = dartType, name = key, isNonNull = false, finalPropertyValue = value, finalPropertyName = key
                )
                properties.add(dartClassProperty)
            }
        }
        return FreezedCovertModel(properties = properties, className = className)
    }


}

val JsonElement.isObject: Boolean
    get() = try {
        this.jsonObject
        true
    } catch (_: Exception) {
        false
    }

val JsonElement.isJsonArray: Boolean
    get() = try {
        this.jsonArray
        true
    } catch (_: Exception) {
        false
    }