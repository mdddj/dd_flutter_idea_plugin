package shop.itbug.fluttercheckversionx.inlay.yaml

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.inlay.getLine
import shop.itbug.fluttercheckversionx.tools.YAML_DART_PACKAGE_INFO_KEY
import shop.itbug.fluttercheckversionx.util.YamlExtends

class PubspecPackageUpdateTimeShowInlay : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {

        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
                if (file !is YAMLFile) return
                val dartInfos = file.getUserData(YAML_DART_PACKAGE_INFO_KEY) ?: return
                val packageInfo = YamlExtends(element).getMyDartPackageModel()
                if (packageInfo != null) {
                    val model = dartInfos.find { it.name == packageInfo.packageName }
                    if (model != null && model.pubData != null && model.getLastUpdate() != null) {
                        sink.addPresentation(
                            EndOfLinePosition(editor.getLine(element)),
                            null,
                            null,
//                            false,
                            HintFormat.default.withColorKind(HintColorKind.TextWithoutBackground)
                                .withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding)
                                .withFontSize(HintFontSize.AsInEditor)
                        ) {
                            this.text(model.getLastUpdateTimeFormatString())

                        }

                        model.getLastVersionText()?.let { lastVersion ->
                            sink.addPresentation(
                                InlineInlayPosition(element.textRange.endOffset, true, 0),
                                null,
                                null,
//                                true,
                                HintFormat.default.withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding)
                                    .withColorKind(HintColorKind.Default).withFontSize(HintFontSize.AsInEditor)
                            ) {
                                this.text(lastVersion)
                            }
                        }
                    }
                }
            }
        }
    }
}