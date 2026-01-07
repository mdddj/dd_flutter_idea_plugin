package shop.itbug.flutterxmcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.*
import shop.itbug.flutterxmcp.config.McpServerConfig
import shop.itbug.flutterxmcp.tools.DartVmTools
import java.net.BindException
import java.net.ServerSocket
import kotlin.coroutines.CoroutineContext

@Service(Service.Level.PROJECT)
class FlutterXMCPService(val project: Project) : CoroutineScope {
    private val job = SupervisorJob()
    private var httpServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val logger = thisLogger()

    // Dart VM 工具集
    private val dartVmTools by lazy { DartVmTools(project) }

    // 当前运行的端口
    private var currentPort: Int? = null

    // 最大重试次数
    private val maxRetryAttempts = 10

    /**
     * 检查端口是否可用
     * @param port 要检查的端口
     * @return true 如果端口可用，false 如果端口被占用
     */
    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 查找可用端口
     * @param startPort 起始端口
     * @param maxAttempts 最大尝试次数
     * @return 可用端口，如果找不到则返回 null
     */
    private fun findAvailablePort(startPort: Int, maxAttempts: Int = maxRetryAttempts): Int? {
        for (i in 0 until maxAttempts) {
            val port = startPort + i
            if (port > 65535) break
            if (isPortAvailable(port)) {
                return port
            }
        }
        return null
    }

    /**
     * 启动 MCP HTTP Server
     * @param port 服务端口，默认从配置读取
     * @param autoFindPort 如果端口被占用，是否自动查找可用端口
     */
    fun startServer(port: Int? = null, autoFindPort: Boolean = true) {
        if (httpServer != null) {
            logger.warn("MCP Server 已经在运行中")
            return
        }

        val configuredPort = port ?: McpServerConfig.getState(project).port

        launch {
            var serverPort = configuredPort
            var started = false

            // 先检查配置的端口是否可用
            if (!isPortAvailable(serverPort)) {
                if (autoFindPort) {
                    logger.info("端口 $serverPort 已被占用，正在查找可用端口...")
                    val availablePort = findAvailablePort(serverPort + 1)
                    if (availablePort != null) {
                        serverPort = availablePort
                        logger.info("找到可用端口: $serverPort")
                    } else {
                        logger.error("无法找到可用端口 (尝试范围: $configuredPort - ${configuredPort + maxRetryAttempts})")
                        return@launch
                    }
                } else {
                    logger.error("端口 $serverPort 已被占用")
                    return@launch
                }
            }

            try {
                httpServer = embeddedServer(CIO, serverPort) {
                    install(SSE)
                    routing {
                        // MCP 端点 - 根路径
                        mcp {
                            createMcpServer()
                        }
                    }
                }
                currentPort = serverPort
                started = true
                logger.info("FlutterX MCP Server 启动在端口: $serverPort")
                logger.info("MCP Endpoint: http://localhost:$serverPort/")

                // 如果端口被自动选择，同步更新配置
                if (serverPort != configuredPort) {
                    ApplicationManager.getApplication().invokeLater {
                        McpServerConfig.getState(project).port = serverPort
                    }
                    logger.info("配置端口已更新为: $serverPort")
                }

                httpServer?.start(wait = true)
            } catch (e: BindException) {
                logger.error("端口绑定失败: ${e.message}")
                httpServer = null
                currentPort = null

                // 如果启动时发生绑定异常，尝试重新查找端口
                if (autoFindPort && !started) {
                    val newPort = findAvailablePort(serverPort + 1)
                    if (newPort != null) {
                        logger.info("重试使用端口: $newPort")
                        delay(100)
                        startServer(newPort, autoFindPort = false) // 避免无限递归
                    }
                }
            } catch (e: Exception) {
                logger.error("启动 MCP Server 失败", e)
                httpServer = null
                currentPort = null
            }
        }
    }

    /**
     * 创建 MCP Server 实例并注册工具
     */
    private fun createMcpServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "flutterx-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(
                        subscribe = true,
                        listChanged = true
                    )
                )
            ),
            {
                "IntelliJ IDEA FlutterX Plugin MCP Server - 提供 Dart VM 调试和监控工具"
            },
        )

        // 注册 Dart VM 相关工具
        dartVmTools.registerTools(server)

        logger.info("已注册 FlutterX MCP 工具")
        return server
    }

    /**
     * 停止 MCP Server
     */
    fun stopServer() {
        httpServer?.stop(1000, 2000)
        httpServer = null
        currentPort = null
        logger.info("FlutterX MCP Server 已停止")
    }

    /**
     * 重启 MCP Server
     * @param newPort 新端口，如果为 null 则使用当前端口或配置端口
     */
    fun restartServer(newPort: Int? = null) {
        val port = newPort ?: currentPort ?: McpServerConfig.getState(project).port
        stopServer()
        launch {
            delay(500) // 等待端口释放
            startServer(port)
        }
    }

    /**
     * 检查服务器是否正在运行
     */
    fun isRunning(): Boolean = httpServer != null

    /**
     * 获取当前运行的端口
     */
    fun getCurrentPort(): Int? = currentPort

    /**
     * 获取服务器状态信息
     */
    fun getStatusInfo(): ServerStatusInfo {
        val configuredPort = McpServerConfig.getState(project).port
        val actualPort = currentPort
        return ServerStatusInfo(
            isRunning = isRunning(),
            port = actualPort,
            configuredPort = configuredPort,
            endpoint = if (isRunning()) "http://localhost:$actualPort/" else null
        )
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default + CoroutineName("FlutterXMCPService")

    companion object {
        fun getInstance(project: Project): FlutterXMCPService {
            return project.getService(FlutterXMCPService::class.java)
        }
    }
}

/**
 * 服务器状态信息
 */
data class ServerStatusInfo(
    val isRunning: Boolean,
    val port: Int?,
    val configuredPort: Int,
    val endpoint: String?
)

/**
 * 项目启动时自动启动 MCP Server（如果配置了自动启动）
 */
class McpServerStartupActivity : ProjectActivity {
    private val logger = thisLogger()

    override suspend fun execute(project: Project) {
        val config = McpServerConfig.getState(project)
        if (config.autoStart) {
            logger.info("自动启动 MCP Server...")
            FlutterXMCPService.getInstance(project).startServer(config.port)
        }
    }
}