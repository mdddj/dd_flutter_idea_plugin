package shop.itbug.flutterx.inlay.yaml

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecPackageContext
import shop.itbug.flutterx.api.inlay.getPubspecInlayProviders
import shop.itbug.flutterx.common.yaml.DartYamlModel
import shop.itbug.flutterx.tools.YAML_DART_PACKAGE_INFO_KEY
import shop.itbug.flutterx.util.YamlExtends
import shop.itbug.flutterx.util.versionType
import javax.swing.JComponent

/** IntelliJ adapter for all FlutterX pubspec inlay extensions. */
class FlutterXPubspecInlayProvider : InlayHintsProvider<NoSettings> {
    override val name: String = "FlutterX pubspec hints"
    override val key: SettingsKey<NoSettings> = SettingsKey("flutterx.pubspec.inlays")
    override val previewText: String = """
        dependencies:
          provider: ^6.1.1
    """.trimIndent()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink,
    ): InlayHintsCollector {
        val yamlFile = (file as? YAMLFile)?.takeIf { it.name == PUBSPEC_FILE_NAME }
        val providers = if (yamlFile == null) emptyList() else getPubspecInlayProviders()

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink,
            ): Boolean {
                val currentFile = yamlFile ?: return false
                val packageContext = createPubspecPackageContext(currentFile, element)
                val context = PubspecInlayContext(
                    file = currentFile,
                    element = element,
                    editor = editor,
                    factory = factory,
                    packageContext = packageContext,
                    sink = sink,
                )

                providers.forEach { provider ->
                    try {
                        provider.collect(context)
                    } catch (exception: ProcessCanceledException) {
                        throw exception
                    } catch (exception: Throwable) {
                        LOG.warn("Pubspec inlay provider failed: ${provider.javaClass.name}", exception)
                    }
                }
                return true
            }
        }
    }

    override fun createSettings(): NoSettings = NoSettings()

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable =
        object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel { }
        }
}

internal fun createPubspecPackageContext(
    file: YAMLFile,
    element: PsiElement,
): PubspecPackageContext? {
    val packageElement = element as? YAMLKeyValueImpl ?: return null
    val yamlExtends = YamlExtends(packageElement)
    if (!yamlExtends.isDartPluginElement()) return null

    val models = file.getUserData(YAML_DART_PACKAGE_INFO_KEY).orEmpty()
    val cachedModel = models.firstOrNull { it.element.element === packageElement }
        ?: models.firstOrNull { it.name == packageElement.keyText }
    val dartYamlModel = cachedModel ?: createLocalDartYamlModel(yamlExtends)

    return PubspecPackageContext(
        name = packageElement.keyText,
        element = packageElement,
        yamlExtends = yamlExtends,
        dartYamlModel = dartYamlModel,
        pubVersionDataModel = dartYamlModel?.pubData,
    )
}

private fun createLocalDartYamlModel(yamlExtends: YamlExtends): DartYamlModel? {
    val packageModel = yamlExtends.getMyDartPackageModel() ?: return null
    val pointerManager = SmartPointerManager.getInstance(packageModel.element.project)
    return DartYamlModel(
        name = packageModel.packageName,
        version = packageModel.detail.version,
        versionType = packageModel.detail.versionType,
        element = pointerManager.createSmartPsiElementPointer(packageModel.element),
        plainText = pointerManager.createSmartPsiElementPointer(packageModel.versionElement),
    )
}

private const val PUBSPEC_FILE_NAME = "pubspec.yaml"
private val LOG = Logger.getInstance(FlutterXPubspecInlayProvider::class.java)
