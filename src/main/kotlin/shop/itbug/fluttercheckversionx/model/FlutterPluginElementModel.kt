package shop.itbug.fluttercheckversionx.model

import com.intellij.psi.PsiElement
///插件的类型
enum class FlutterPluginType(val type: String){
    dependencies("dependencies"),devDependencies("dev_dependencies"),dependencyOverrides("dependency_overrides")
}
///flutter插件节点
data class FlutterPluginElementModel(val name: String,val element: PsiElement,val type: FlutterPluginType)