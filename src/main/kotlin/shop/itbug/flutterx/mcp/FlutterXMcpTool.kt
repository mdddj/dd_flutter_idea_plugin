package shop.itbug.flutterx.mcp

import com.intellij.mcpserver.impl.McpServerService


object FlutterXMcpTool {

    fun isEnabled(): Boolean {
        val mcpSetting = McpServerService.getInstance()
        return mcpSetting.isRunning
    }
}