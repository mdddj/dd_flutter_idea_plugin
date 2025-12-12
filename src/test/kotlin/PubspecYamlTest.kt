
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import shop.itbug.flutterx.model.FlutterPluginType
import shop.itbug.flutterx.util.PubspecYamlElementFactory


private val fileText = """
name: demo
publish_to: 'none' 
environment:
  sdk: ^3.9.0

dependencies:
  flutter:
    sdk: flutter

  cupertino_icons: ^1.0.8
  provider: ^6.1.1
  http: ^1.1.0
  shared_preferences: ^2.5.3
  freezed_annotation: ^3.1.0
  freezed: ^3.2.2
  hooks_riverpod: ^2.6.1
  flutter_hooks: ^0.21.3+1
  riverpod_annotation: ^2.6.1
  path_provider: ^2.1.5
  flex_color_scheme: ^8.3.0
  dd_js_util: 
    path: ../dd_js_util
""".trimIndent()


class PubspecYamlTest : BasePlatformTestCase() {


    fun findKeyValue(key: String, file: YAMLFile): YAMLKeyValue? {
        return PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java).find { it.key?.text == key }
    }

    fun generateKeyValue(key: String, value: String): YAMLKeyValue {
        return YAMLElementGenerator.getInstance(project).createYamlKeyValue(key, value)
    }

    fun createTop(key: String): YAMLKeyValue {
        val keyValue = YAMLElementGenerator.getInstance(project)
            .createYamlKeyValueWithSequence(
                key, mapOf(
                    "test" to "0.0.1"
                )
            )
        return keyValue!!

    }


    //模拟添加依赖
    fun testAddDeps() {
        val yamlFile = myFixture.configureByText("pubspec.yaml", fileText) as YAMLFile

        val factory = PubspecYamlElementFactory(project = project)


        yamlFile.documents.firstOrNull()?.let {
            val mp = it.topLevelValue as? YAMLMapping?
            if (mp != null) {
                val last = mp.keyValues.last()
                WriteCommandAction.runWriteCommandAction(project) {
                    val newLast = mp.addAfter(factory.createEol(), last)
                    mp.addAfter(createTop(FlutterPluginType.OverridesDependencies.type), newLast)
                }
            }
        }

        factory.addDependencies(
            "provider",
            "^0.0.1",
            yamlFile,
            FlutterPluginType.OverridesDependencies
        )


        println(yamlFile.text)


    }

    fun testPathGet(){
        val yamlFile = myFixture.configureByText("pubspec.yaml", fileText) as YAMLFile






    }


}