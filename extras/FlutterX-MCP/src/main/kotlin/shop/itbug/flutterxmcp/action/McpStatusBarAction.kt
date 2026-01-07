package shop.itbug.flutterxmcp.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import shop.itbug.flutterxmcp.FlutterXMCPService
import shop.itbug.flutterxmcp.config.McpServerConfig
import java.awt.datatransfer.StringSelection

/**
 * MCP Server 状态栏 Action
 * 显示 MCP 状态，点击启动/重启并复制配置到剪贴板
 */
class McpStatusBarAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val mcpService = FlutterXMCPService.getInstance(project)
        val isRunning = mcpService.isRunning()
        val port = mcpService.getCurrentPort() ?: McpServerConfig.getState(project).port

        e.presentation.isEnabledAndVisible = true
        e.presentation.text = if (isRunning) {
            "MCP: Running ($port)"
        } else {
            "MCP: Stopped"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val mcpService = FlutterXMCPService.getInstance(project)

        if (mcpService.isRunning()) {
            // 已启动，重启
            mcpService.restartServer()
        } else {
            // 未启动，启动
            mcpService.startServer()
        }

        // 复制 MCP 配置到剪贴板
        val port = mcpService.getCurrentPort() ?: McpServerConfig.getState(project).port
        val mcpConfig = """
{
    "flutterx": {
      "type": "sse",
      "url": "http://127.0.0.1:$port/"
    }
}
        """.trimIndent()

        CopyPasteManager.getInstance().setContents(StringSelection(mcpConfig))
    }
}
