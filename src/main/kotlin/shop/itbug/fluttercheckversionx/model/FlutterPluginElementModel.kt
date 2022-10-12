package shop.itbug.fluttercheckversionx.model

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

///插件的类型
enum class FlutterPluginType(val type: String){
    dependencies("dependencies"),devDependencies("dev_dependencies"),dependencyOverrides("dependency_overrides")
}
///flutter插件节点
data class FlutterPluginElementModel(val name: String,val element: PsiElement,val type: FlutterPluginType,var pubData: PubVersionDataModel? = null)

/**
 * 判断一下是否为最新版本
 * @return [true] 表示为最新版本 [false] - 不是最新版本
 */
fun FlutterPluginElementModel.isLastVersion():Boolean {

    //忽略git和本地依赖
    if(element.lastChild is YAMLBlockMappingImpl){
        return true
    }
    if(pubData!=null){
      val lastVersion =  pubData!!.latest.version
        if(!getElementVersion().contains(lastVersion)){
            return false
        }
    }
    return true;
}

/**
 * 获取文件element节点上的版本
 */
fun FlutterPluginElementModel.getElementVersion() :String{
   return runReadAction { (element as YAMLKeyValueImpl).valueText }
}