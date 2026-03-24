package vm.drift

import com.google.gson.JsonObject
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

/**
 * Drift 数据库基础信息
 */
data class DriftDatabase(
    val id: Int,
    val name: String,
    val dateTimeAsText: Boolean,
    val tables: List<DriftTable>
)

/**
 * Drift 表信息
 */
data class DriftTable(
    val name: String,
    val type: String, // table, view, virtual_table
    val columns: List<DriftColumn>
)

/**
 * Drift 列信息
 */
data class DriftColumn(
    val name: String,
    val type: String, // TEXT, INTEGER, DOUBLE, BOOL, DATE, BLOB, etc.
    val isNullable: Boolean
)

/**
 * Drift 数据行
 */
data class DriftRow(
    val data: Map<String, Any?>
)

/**
 * SQL 执行结果
 */
data class DriftQueryResult(
    val columns: List<String>,
    val rows: List<DriftRow>
)

/**
 * 将查询结果转换为 CSV 字符串
 */
fun DriftQueryResult.toCsv(): String {
    val header = columns.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
    val dataRows = rows.joinToString("\n") { row ->
        columns.joinToString(",") { col ->
            val value = row.data[col]?.toString() ?: ""
            "\"${value.replace("\"", "\"\"")}\""
        }
    }
    return "$header\n$dataRows"
}


/**
 * 在编辑器中打开虚拟的 csv文件
 */
fun DriftQueryResult.openInEditor(project: Project) {
    val csvContent = toCsv()
    val fileName = "Query_Result_${System.currentTimeMillis()}.csv"
    val virtualFile = LightVirtualFile(fileName, csvContent)
    FileEditorManager.getInstance(project).openFile(virtualFile, true)

}


/**
 * Drift 支持的 SQL 类型
 */
enum class DriftSqlType {
    TEXT, INTEGER, DOUBLE, BOOL, DATE, BLOB, UNSUPPORTED
}

/**
 * 解析从 Dart 端返回的数据库描述 JSON
 */
fun parseDatabaseDescription(id: Int, dbName: String, json: JsonObject): DriftDatabase {
    val dateTimeAsText = json.get("dateTimeAsText")?.asBoolean ?: false
    val entities = json.getAsJsonArray("entities")
    val tables = entities?.map { entity ->
        val entityObj = entity.asJsonObject
        val tableName = entityObj.get("name").asString
        val tableType = entityObj.get("type").asString
        val columnsArray = entityObj.getAsJsonArray("columns")
        val columns = columnsArray?.map { column ->
            val colObj = column.asJsonObject
            val colName = colObj.get("name").asString
            val typeObj = colObj.getAsJsonObject("type")
            val typeStr = typeObj.get("type")?.asString ?: typeObj.get("customTypeName")?.asString ?: "UNKNOWN"
            val isNullable = colObj.get("isNullable").asBoolean
            DriftColumn(colName, typeStr, isNullable)
        } ?: emptyList()
        DriftTable(tableName, tableType, columns)
    } ?: emptyList()

    return DriftDatabase(id, dbName, dateTimeAsText, tables)
}

/**
 * 筛选操作符
 */
enum class DriftFilterOperator(val label: String, val sql: String) {
    Equals("=", "="),
    GreaterThan(">", ">"),
    LessThan("<", "<"),
    Contains("Contains", "LIKE"),
    IsNo("Is No", "="),
    IsYes("Is Yes", "="),
    Before("Before", "<"),
    After("After", ">"),
    OnDate("On Date", "=")
}

/**
 * 筛选条件
 */

data class DriftFilter(
    val columnName: String,
    val columnType: String,
    val operator: DriftFilterOperator,
    val value: String
)

/**
 * 排序条件
 */
data class DriftOrderBy(
    val columnName: String,
    val isAscending: Boolean = true
)

