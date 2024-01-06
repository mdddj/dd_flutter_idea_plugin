package shop.itbug.fluttercheckversionx.model

import com.intellij.openapi.application.runReadAction
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName

///插件的类型
enum class FlutterPluginType(val type: String, val title: String) {
    Dependencies("dependencies", "Default"), DevDependencies(
        "dev_dependencies",
        "Dev"
    ),
    DependencyOverrides("dependency_overrides", "Overrides")
}


val FlutterPluginElementModel.dartPluginModel get() = DartPluginVersionName(name, getElementVersion())

///flutter插件节点
data class FlutterPluginElementModel(
    val name: String,
    val element: YAMLKeyValueImpl,
    val type: FlutterPluginType,
    var pubData: PubVersionDataModel? = null
)


/**
 * 获取文件element节点上的版本
 */
fun FlutterPluginElementModel.getElementVersion(): String {
    return runReadAction { element.valueText }
}