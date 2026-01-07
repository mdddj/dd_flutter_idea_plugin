package shop.itbug.flutterxmcp.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * MCP Server 配置状态
 */
data class McpServerState(
    var port: Int = 5990,
    var autoStart: Boolean = false
)

/**
 * MCP Server 配置服务
 * 项目级别的持久化配置
 */
@Service(Service.Level.PROJECT)
@State(
    name = "FlutterXMcpServerConfig",
    storages = [Storage("flutterx-mcp.xml")]
)
class McpServerConfig : PersistentStateComponent<McpServerState> {

    private var state = McpServerState()

    override fun getState(): McpServerState = state

    override fun loadState(state: McpServerState) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): McpServerConfig {
            return project.service<McpServerConfig>()
        }

        fun getState(project: Project): McpServerState {
            return getInstance(project).state
        }
    }
}
