package shop.itbug.flutterx.api.inlay

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.flutterx.common.yaml.DartYamlModel
import shop.itbug.flutterx.model.PubVersionDataModel
import shop.itbug.flutterx.util.YamlExtends

/**
 * FlutterX extension for adding inlays to pubspec.yaml.
 *
 * [PubspecInlayContext.packageContext] is present only when the current PSI
 * element is a package declared under a supported dependency section.
 */
interface PubspecInlayProvider : DumbAware {
    fun collect(context: PubspecInlayContext)
}

class PubspecInlayContext internal constructor(
    val file: YAMLFile,
    val element: PsiElement,
    val editor: Editor,
    val factory: PresentationFactory,
    val packageContext: PubspecPackageContext?,
    private val sink: InlayHintsSink,
) {
    fun centerVertically(presentation: InlayPresentation): InlayPresentation {
        val centeredOffset = ((editor.lineHeight - presentation.height) / 2).coerceAtLeast(0)
        val baselineOffset = if (centeredOffset > 0) 1 else 0
        return factory.inset(
            presentation,
            top = centeredOffset + baselineOffset,
        )
    }

    fun addInlineElement(
        presentation: InlayPresentation,
        offset: Int = element.textRange.endOffset,
        relatesToPrecedingText: Boolean = false,
        placeAtTheEndOfLine: Boolean = false,
    ) {
        sink.addInlineElement(
            offset,
            relatesToPrecedingText,
            presentation,
            placeAtTheEndOfLine,
        )
    }
}

data class PubspecPackageContext(
    val name: String,
    val element: YAMLKeyValueImpl,
    val yamlExtends: YamlExtends,
    val dartYamlModel: DartYamlModel?,
    val pubVersionDataModel: PubVersionDataModel?,
)

private val PUBSPEC_INLAY_PROVIDER_EP =
    ExtensionPointName.create<PubspecInlayProvider>(
        "shop.itbug.FlutterCheckVersionX.pubspecInlayProvider"
    )

internal fun getPubspecInlayProviders(): List<PubspecInlayProvider> =
    PUBSPEC_INLAY_PROVIDER_EP.extensionList
