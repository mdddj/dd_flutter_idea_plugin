package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import shop.itbug.flutterx.mcp.truncateForMcp
import vm.drift.DriftAsyncState
import vm.drift.DriftDatabase
import vm.drift.DriftServices

/**
 * Drift 面板：列出已追踪的数据库和表，并读取一张表的行。
 * 只发 SELECT，不提供改数据的入口。
 */
class DartVmDriftMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read Drift databases registered with TrackedDatabase in the running Flutter app. Omit database and table to list databases, tables, and columns. Pass database (name or id) and table to run SELECT * with a row limit. This tool only reads. limit defaults to 20 and is capped at 100."
    )
    suspend fun flutterx_drift(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Drift database name or numeric id. Omit it to list every database, or to use the first database when table is set.") database: String? = null,
        @McpDescription("Exact table name from the database schema. Omit it to return schema only.") table: String? = null,
        @McpDescription("Maximum rows to return. Default 20, maximum 100.") limit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val services = DriftServices(app.vmService)
        try {
            services.fetchDatabases()
            val databases = when (val state = services.state.value.databases) {
                is DriftAsyncState.Data -> state.data
                is DriftAsyncState.Error -> mcpFail("Cannot read Drift databases: ${state.error.message}")
                DriftAsyncState.Loading -> mcpFail("Drift database request did not finish")
            }
            if (table.isNullOrBlank()) {
                return@guard mapOf(
                    "app" to app.toMcpSummary(),
                    "count" to databases.size,
                    "databases" to databases.map { it.toMcpSchema() },
                    "hint" to "Pass database and table to read rows.",
                ).toMcpJson()
            }

            val db = resolveDatabase(databases, database)
            val driftTable = db.tables.firstOrNull { it.name == table }
                ?: mcpFail("Unknown table '$table' in '${db.name}'. Tables: ${db.tables.joinToString { it.name }}")
            val rowLimit = clampCount(limit, defaultValue = 20, maxValue = 100)
            services.selectDatabase(db)
            services.updateLimit(rowLimit)
            services.selectTable(driftTable)
            val rows = when (val result = services.state.value.queryResult) {
                is DriftAsyncState.Data -> result.data
                is DriftAsyncState.Error -> mcpFail("Cannot read Drift table '$table': ${result.error.message}")
                else -> mcpFail("Drift query for '$table' did not finish")
            }
            mapOf(
                "app" to app.toMcpSummary(),
                "database" to db.toMcpSchema(),
                "table" to table,
                "columns" to rows.columns,
                "returned" to rows.rows.size,
                "rows" to rows.rows.map { row ->
                    row.data.mapValues { (_, value) ->
                        when (value) {
                            is String -> value.truncateForMcp(500)
                            else -> value
                        }
                    }
                },
            ).toMcpJson()
        } finally {
            services.dispose()
        }
    }

    private fun resolveDatabase(databases: List<DriftDatabase>, database: String?): DriftDatabase {
        if (databases.isEmpty()) mcpFail("No Drift database is registered. Open a database in the app first.")
        if (database.isNullOrBlank()) {
            return databases.firstOrNull() ?: mcpFail("No Drift database is registered")
        }
        return databases.firstOrNull { it.name == database || it.id.toString() == database }
            ?: mcpFail("Unknown database '$database'. Databases: ${databases.joinToString { "${it.name} (${it.id})" }}")
    }

    private fun DriftDatabase.toMcpSchema(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "name" to name,
        "tables" to tables.map { table ->
            linkedMapOf(
                "name" to table.name,
                "type" to table.type,
                "columns" to table.columns.map { column ->
                    linkedMapOf(
                        "name" to column.name,
                        "type" to column.type,
                        "nullable" to column.isNullable,
                    )
                },
            )
        },
    )
}
