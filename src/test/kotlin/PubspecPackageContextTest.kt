import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.flutterx.common.yaml.DartYamlModel
import shop.itbug.flutterx.inlay.yaml.createPubspecPackageContext
import shop.itbug.flutterx.model.Latest
import shop.itbug.flutterx.model.PubVersionDataModel
import shop.itbug.flutterx.model.Pubspec
import shop.itbug.flutterx.tools.YAML_DART_PACKAGE_INFO_KEY

class PubspecPackageContextTest : BasePlatformTestCase() {
    fun testProvidesPackageModelsFromUnifiedContext() {
        val file = configurePubspec()
        val packageElement = findKeyValue(file, "provider")
        val pointer = SmartPointerManager.getInstance(project)
            .createSmartPsiElementPointer(packageElement)
        val parsedModel = runBlocking { DartYamlModel.create(pointer) }
        assertNotNull(parsedModel)

        val pubData = createPubVersionData("provider")
        val modelWithPubData = parsedModel!!.copy(pubData = pubData)
        file.putUserData(YAML_DART_PACKAGE_INFO_KEY, listOf(modelWithPubData))

        val context = createPubspecPackageContext(file, packageElement)
        assertNotNull(context)
        assertEquals("provider", context!!.name)
        assertSame(packageElement, context.element)
        assertSame(packageElement, context.yamlExtends.element)
        assertSame(modelWithPubData, context.dartYamlModel)
        assertSame(pubData, context.pubVersionDataModel)
    }

    fun testOnlyDependencyPackagesReceivePackageContext() {
        val file = configurePubspec()
        val pathPackage = findKeyValue(file, "local_package")

        val pathContext = createPubspecPackageContext(file, pathPackage)
        assertNotNull(pathContext)
        assertTrue(pathContext!!.yamlExtends.isPathElement())
        assertNull(pathContext.dartYamlModel)
        assertNull(pathContext.pubVersionDataModel)

        assertNull(createPubspecPackageContext(file, findKeyValue(file, "dependencies")))
        assertNull(createPubspecPackageContext(file, findKeyValue(file, "flutter")))
        assertNull(createPubspecPackageContext(file, findKeyValue(file, "path")))
        assertNull(createPubspecPackageContext(file, findKeyValue(file, "name")))

        val devPackage = findKeyValue(file, "build_runner")
        val devContext = createPubspecPackageContext(file, devPackage)
        assertEquals("build_runner", devContext?.name)
        assertEquals("^2.4.0", devContext?.dartYamlModel?.version)
        assertNull(devContext?.pubVersionDataModel)
    }

    private fun configurePubspec(): YAMLFile {
        return myFixture.configureByText(
            "pubspec.yaml",
            """
                name: demo
                environment:
                  sdk: ^3.9.0
                dependencies:
                  flutter:
                    sdk: flutter
                  provider: ^6.1.1
                  local_package:
                    path: ../local_package
                dev_dependencies:
                  build_runner: ^2.4.0
            """.trimIndent(),
        ) as YAMLFile
    }

    private fun findKeyValue(file: YAMLFile, key: String): YAMLKeyValueImpl {
        return PsiTreeUtil.findChildrenOfType(file, YAMLKeyValueImpl::class.java)
            .first { it.keyText == key }
    }

    private fun createPubVersionData(name: String): PubVersionDataModel {
        val pubspec = Pubspec(
            name = name,
            version = "6.1.2",
            homepage = null,
            description = "Test package",
            environment = null,
            dependencies = null,
            repository = null,
            devDependencies = null,
        )
        val latest = Latest(
            version = "6.1.2",
            pubspec = pubspec,
            description = "Test package",
            archiveURL = "https://example.test/$name.zip",
            published = "2026-08-01T00:00:00.000Z",
        )
        return PubVersionDataModel(
            name = name,
            latest = latest,
            versions = emptyList(),
            jsonText = "{}",
            isDiscontinued = false,
        )
    }
}
