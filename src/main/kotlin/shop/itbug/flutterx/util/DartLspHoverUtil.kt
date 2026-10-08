package shop.itbug.flutterx.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.dartlsp.api.LspServer
import com.intellij.platform.dartlsp.api.LspServerManager
import com.intellij.platform.dartlsp.api.LspServerState
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.lsp.DartLspServerSupportProvider
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.MarkedString
import org.eclipse.lsp4j.Position

/** A small, structured view of the Dart LSP hover data used by FlutterX. */
data class DartLspHoverInfo(
    val staticType: String?,
    val content: String,
)

object DartLspHoverUtil {
    private const val REQUEST_TIMEOUT_MS = 1_000
    private val typeLine = Regex("""(?m)^\s*Type:\s*`?([^`\r\n]+?)`?\s*$""")

    fun getHover(element: PsiElement): DartLspHoverInfo? {
        val file = element.containingFile?.virtualFile ?: return null
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        val offset = element.textOffset
        if (offset !in 0..document.textLength) return null

        val server = findRunningServer(element.project, file) ?: return null
        val hover = requestHover(server, file, document, offset) ?: return null
        return parseHover(hover)
    }

    internal fun parseHover(hover: Hover): DartLspHoverInfo? {
        val content = hover.contents ?: return null
        val text = if (content.isLeft) {
            content.left.orEmpty().joinToString("\n") { part ->
                if (part.isLeft) {
                    part.left.orEmpty()
                } else {
                    markedStringText(part.right)
                }
            }
        } else {
            content.right?.value.orEmpty()
        }.trim()

        if (text.isEmpty()) return null
        return DartLspHoverInfo(
            staticType = typeLine.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() },
            content = text,
        )
    }

    private fun findRunningServer(project: Project, file: VirtualFile): LspServer? {
        return LspServerManager.getInstance(project)
            .getServersForProvider(DartLspServerSupportProvider::class.java)
            .firstOrNull { server ->
                server.state == LspServerState.Running && server.descriptor.isSupportedFile(file)
            }
    }

    private fun requestHover(
        server: LspServer,
        file: VirtualFile,
        document: Document,
        offset: Int,
    ): Hover? {
        val line = document.getLineNumber(offset)
        val position = Position(line, offset - document.getLineStartOffset(line))
        val params = HoverParams(server.getDocumentIdentifier(file), position)
        return try {
            server.sendRequestSync<Hover?>(REQUEST_TIMEOUT_MS) { languageServer ->
                languageServer.textDocumentService.hover(params)
            }
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun markedStringText(markedString: MarkedString?): String {
        if (markedString == null) return ""
        val value = markedString.value.orEmpty()
        return if (markedString.language.isNullOrBlank()) {
            value
        } else {
            "```" + markedString.language + "\n$value\n```"
        }
    }
}
