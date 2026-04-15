package shop.itbug.flutterx.services

import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.flutterx.util.DartPluginVersionName



data class MyDartPackage(
    var packageName: String,
    val element: YAMLKeyValueImpl,
    val detail: DartPluginVersionName,
    val versionElement: YAMLPlainTextImpl,
    val error: String? = null,
)

