package shop.itbug.fluttercheckversionx.services.impl

import cn.hutool.core.lang.Console
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.model.DartClassProperty
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.ModelToFreezedModelService
import shop.itbug.fluttercheckversionx.util.DartJavaCovertUtil
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.formatDartName
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

    override fun jsonObjectToFreezedCovertModelList(
        jsonObject: JSONObject,
        oldList: MutableList<FreezedCovertModel>,
        className: String
    ): List<FreezedCovertModel> {
        val rootModel = jsonObjectToFreezedCovertModel(jsonObject,className)
        oldList.add(rootModel)
        jsonObject.forEach { key,value ->
            if(value is JSONObject) {
                val jsonObjectToFreezedCovertModelList = jsonObjectToFreezedCovertModelList(value, oldList,key.toString().formatDartName())
                oldList.addAll(jsonObjectToFreezedCovertModelList)
            }else if(value is JSONArray) {
                if(value.isNotEmpty()) {
                    val firstObject = value.first()
                    println(firstObject::class.java)
                    if(firstObject is JSONObject) {
                        try{
                            val parse = JSONObject.parse(JSONObject.toJSONString(value.first()))
                            val jsonObjectToFreezedCovertModelList = jsonObjectToFreezedCovertModelList(parse, oldList,key.toString().formatDartName())
                            oldList.addAll(jsonObjectToFreezedCovertModelList)
                        }catch (e: Exception){
                            Console.log("数组转失败:${e}")
                        }
                    }

                }
            }
        }
        return oldList
    }

    override fun jsonObjectToFreezedCovertModel(jsonObject: JSONObject, className: String): FreezedCovertModel {
        val properties = mutableListOf<DartClassProperty>()
        jsonObject.forEach { key, value ->
            val dartType = DartJavaCovertUtil.getDartType(value, key)
            val dartClassProperty = DartClassProperty(type = dartType, name = key, isNonNull = false)
            properties.add(dartClassProperty)
        }
        return FreezedCovertModel(properties = properties, className = className)
    }


}