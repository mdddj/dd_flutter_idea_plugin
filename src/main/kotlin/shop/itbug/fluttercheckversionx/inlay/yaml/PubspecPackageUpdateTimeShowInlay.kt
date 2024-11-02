package shop.itbug.fluttercheckversionx.inlay.yaml

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import shop.itbug.fluttercheckversionx.inlay.getLine
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.util.YamlExtends

class PubspecPackageUpdateTimeShowInlay : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {

        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
                val project = element.project
                val service = DartPackageCheckService.getInstance(project)
                val packageInfo = YamlExtends(element).getMyDartPackageModel()
                if (packageInfo != null) {
                    val model = service.findPackageInfoByName(packageInfo.packageName)
                    if (model != null) {
                        sink.addPresentation(
                            EndOfLinePosition(editor.getLine(element)),
                            null,
                            null,
                            HintFormat.default.withColorKind(HintColorKind.TextWithoutBackground)
                                .withHorizontalMargin(HintMarginPadding.MarginAndSmallerPadding)
                                .withFontSize(HintFontSize.AsInEditor)
                        ) {
                            this.text(model.getLastUpdateTimeFormatString())
                        }

                        model.serverLastVersion()?.let { lastVersion ->
                            sink.addPresentation(
                                InlineInlayPosition(element.textRange.endOffset, true, 0),
                                null,
                                null,
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