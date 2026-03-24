package shop.itbug.flutterxmcp.setting

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import shop.itbug.flutterxmcp.FlutterXMCPService
import shop.itbug.flutterxmcp.config.McpServerConfig
import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.Timer

/**
 * MCP Server 设置页面
 * 作为 FlutterX 设置的子设置页面
 */
class McpServerSetting(private val project: Project) : Configurable {

    private lateinit var myPanel: DialogPanel
    private val config = McpServerConfig.getState(project)
    private val mcpService by lazy { FlutterXMCPService.getInstance(project) }

    // UI 组件
    private lateinit var statusLabel: JBLabel
    private lateinit var portLabel: JBLabel
    private lateinit var startButton: JButton
    private lateinit var stopButton: JButton
    private lateinit var restartButton: JButton

    // 定时器用于刷新状态
    private var statusTimer: Timer? = null

    override fun getDisplayName(): String = "MCP Server"

    override fun createComponent(): JComponent {
        myPanel = panel {
            // 服务器状态区域
            group("Server Status") {
                row {
                    label("Status:")
                    statusLabel = JBLabel().apply {
                        font = font.deriveFont(Font.BOLD)
                    }
                    cell(statusLabel)
                }
                row {
                    label("Running Port:")
                    portLabel = JBLabel("-")
                    cell(portLabel)
                }
            }

            // 服务器配置区域
            group("Server Configuration") {
                row("Port:") {
                    intTextField(1024..65535)
                        .bindIntText(config::port)
                        .comment("MCP Server listening port (1024-65535), default: 5990")
                        .align(Align.FILL)
                }
                row {
                    checkBox("Auto start on project open")
                        .bindSelected(config::autoStart)
                        .comment("Automatically start MCP Server when the project is opened")
                }
            }

            // 服务器控制区域
            group("Server Control") {
                row {
                    startButton = JButton("Start Server").apply {
                        addActionListener { startServer() }
                    }
                    cell(startButton)

                    stopButton = JButton("Stop Server").apply {
                        addActionListener { stopServer() }
                    }
                    cell(stopButton)

                    restartButton = JButton("Restart Server").apply {
                        addActionListener { restartServer() }
                    }
                    cell(restartButton)
                }
            }

            // 连接信息区域
            group("Connection Info") {
                row {
                    comment(
                        """
                        <b>MCP Endpoint:</b> http://localhost:${config.port}/<br/>
                        <br/>
                        <b>MCP Client Configuration (mcp.json):</b><br/>
                        <code>
                        {<br/>
                        &nbsp;&nbsp;"mcpServers": {<br/>
                        &nbsp;&nbsp;&nbsp;&nbsp;"flutterx": {<br/>
                        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type": "sse",<br/>
                        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"url": "http://127.0.0.1:${config.port}/"<br/>
                        &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
                        &nbsp;&nbsp;}<br/>
                        }
                        </code>
                        <br/><br/>
                        <b>Available Tools:</b><br/>
                        • get_running_flutter_apps - Get running Flutter apps<br/>
                        • get_dart_vm_info - Get Dart VM information<br/>
                        • get_http_requests - Get HTTP request monitoring data<br/>
                        • get_http_request_detail - Get HTTP request details<br/>
                        • get_shared_preferences_keys - Get SharedPreferences keys<br/>
                        • get_shared_preferences_value - Get SharedPreferences value<br/>
                        • get_current_project_name - Get current project name
                    """.trimIndent()
                    )
                }
            }
        }

        // 初始化状态显示
        updateStatusDisplay()

        // 启动定时刷新
        startStatusTimer()

        return myPanel
    }

    private fun startServer() {
        myPanel.apply() // 先应用配置
        mcpService.startServer(config.port)
        // 延迟更新状态，等待服务器启动
        Timer(500) {
            updateStatusDisplay()
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun stopServer() {
        mcpService.stopServer()
        updateStatusDisplay()
    }

    private fun restartServer() {
        mcpService.stopServer()
        Timer(500) {
            mcpService.startServer(config.port)
            Timer(500) {
                updateStatusDisplay()
            }.apply {
                isRepeats = false
                start()
            }
        }.apply {
            isRepeats = false
            start()
        }
    }

    private fun updateStatusDisplay() {
        val statusInfo = mcpService.getStatusInfo()
        if (statusInfo.isRunning) {
            statusLabel.text = "Running"
            statusLabel.foreground = JBColor(0x2E7D32, 0x81C784) // 绿色
            statusLabel.icon = AllIcons.General.InspectionsOK
            portLabel.text = statusInfo.port.toString()
            portLabel.foreground = JBColor.foreground()
        } else {
            statusLabel.text = "Stopped"
            statusLabel.foreground = JBColor(0xC62828, 0xEF5350) // 红色
            statusLabel.icon = AllIcons.General.Error
            portLabel.text = "-"
            portLabel.foreground = JBColor.foreground()
        }

        // 更新按钮状态
        startButton.isEnabled = !statusInfo.isRunning
        stopButton.isEnabled = statusInfo.isRunning
        restartButton.isEnabled = statusInfo.isRunning
    }

    private fun startStatusTimer() {
        statusTimer = Timer(2000) {
            updateStatusDisplay()
        }.apply {
            isRepeats = true
            start()
        }
    }

    private fun stopStatusTimer() {
        statusTimer?.stop()
        statusTimer = null
    }

    override fun isModified(): Boolean = myPanel.isModified()

    override fun apply() {
        myPanel.apply()
        McpServerConfig.getInstance(project).loadState(config)
    }

    override fun reset() {
        myPanel.reset()
    }

    override fun disposeUIResources() {
        stopStatusTimer()
        super.disposeUIResources()
    }
}
