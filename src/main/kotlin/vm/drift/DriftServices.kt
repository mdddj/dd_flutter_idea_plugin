package vm.drift

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vm.VmService
import vm.VmServiceListener
import vm.consumer.EvaluateConsumer
import vm.consumer.GetObjectConsumer
import vm.consumer.ServiceExtensionConsumer
import vm.element.*
import vm.getVm
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Drift 异步状态封装
 */
sealed class DriftAsyncState<out T> {
    data object Loading : DriftAsyncState<Nothing>()
    data class Data<T>(val data: T) : DriftAsyncState<T>()
    data class Error(val error: Throwable) : DriftAsyncState<Nothing>()
}

/**
 * Drift 状态
 */
data class DriftState(
    val databases: DriftAsyncState<List<DriftDatabase>> = DriftAsyncState.Loading,
    val selectedDatabase: DriftDatabase? = null,
    val selectedTable: DriftTable? = null,
    val queryResult: DriftAsyncState<DriftQueryResult>? = null,
    val logs: List<String> = emptyList(),
    val selectedColumns: Set<String> = emptySet(),
    val filters: List<DriftFilter> = emptyList(),
    val orderBy: List<DriftOrderBy> = emptyList(),
    val limit: Int = 100
)

/**
 * Drift 服务类
 */
class DriftServices(val vmService: VmService) : CoroutineScope, VmServiceListener {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val _state = MutableStateFlow(DriftState())
    val state: StateFlow<DriftState> = _state.asStateFlow()

    private var driftLibraryId: String? = null

    init {
        vmService.addVmServiceListener(this)
    }

    override fun connectionOpened() {}
    override fun connectionClosed() {}
    override fun received(streamId: String, event: Event) {
        if (streamId == VmService.EXTENSION_STREAM_ID && event.getExtensionKind() == "drift:database-list-changed") {
            launch { fetchDatabases() }
        }
    }

    /**
     * 获取主 Isolate ID
     */
    private suspend fun getMainIsolateId(): String {
        return vmService.getMainIsolateId().ifBlank {
            val vm = vmService.getVm()
            val mainIsolate = vm.getIsolates().find { it.getName() == "main" }
            mainIsolate?.getId() ?: throw IllegalStateException("Main isolate not found")
        }
    }

    /**
     * 获取 drift 库 ID (包含 TrackedDatabase)
     */
    private suspend fun getDriftLibraryId(isolateId: String): String {
        driftLibraryId?.let { return it }
        val isolate = vmService.getIsolateByIdPub(isolateId)
            ?: throw IllegalStateException("Could not get isolate details")
        val libraries = isolate.getLibraries()
        val lib = libraries.find { it.getUri()?.contains("package:drift/src/runtime/devtools/devtools.dart") == true }
            ?: throw IllegalStateException("Drift devtools library not found")
        driftLibraryId = lib.getId()
        return driftLibraryId!!
    }

    /**
     * 刷新数据库列表
     */
    suspend fun fetchDatabases() {
        _state.value = _state.value.copy(databases = DriftAsyncState.Loading)
        try {
            val isolateId = getMainIsolateId()
            val libId = getDriftLibraryId(isolateId)
            
            // 获取所有追踪的数据库 ID 和名称
            val expression = "json.encode(TrackedDatabase.all.map((e) => {'id': e.id, 'name': e.database.runtimeType.toString()}).toList())"
            val result = evaluateExpression(isolateId, libId, expression)
            
            val dbList = mutableListOf<DriftDatabase>()
            val dbArray = vmService.gson.fromJson(result.asString, JsonArray::class.java)
            for (item in dbArray) {
                val obj = item.asJsonObject
                val id = obj.get("id").asInt
                val name = obj.get("name").asString
                
                // 获取数据库详细描述
                val descExpr = "describe(TrackedDatabase.all.firstWhere((e) => e.id == $id).database)"
                val descResult = evaluateExpression(isolateId, libId, descExpr)
                // descResult 是一个 JSON 字符串，包含 DatabaseDescription
                val descJson = vmService.gson.fromJson(descResult.asString, JsonObject::class.java)
                dbList.add(parseDatabaseDescription(id, name, descJson))
            }
            
            _state.value = _state.value.copy(databases = DriftAsyncState.Data(dbList))
            addLog("Fetched ${dbList.size} databases")
        } catch (e: Throwable) {
            _state.value = _state.value.copy(databases = DriftAsyncState.Error(e))
            addLog("Error fetching databases: ${e.message}")
        }
    }

    /**
     * 选择数据库
     */
    fun selectDatabase(db: DriftDatabase) {
        _state.value = _state.value.copy(
            selectedDatabase = db,
            selectedTable = null,
            queryResult = null,
            selectedColumns = emptySet(),
            filters = emptyList(),
            orderBy = emptyList()
        )
    }

    /**
     * 选择表并加载数据
     */
    suspend fun selectTable(table: DriftTable) {
        _state.value = _state.value.copy(
            selectedTable = table,
            selectedColumns = table.columns.map { it.name }.toSet(),
            filters = emptyList(),
            orderBy = emptyList()
        )
        executeSelect(table.name)
    }

    /**
     * 执行简单的 SELECT *
     */
    suspend fun executeSelect(tableName: String) {
        val db = _state.value.selectedDatabase ?: return
        val selectedCols = _state.value.selectedColumns
        val limit = _state.value.limit
        
        val colsStr = if (selectedCols.isEmpty()) "*" else selectedCols.joinToString(", ")
        var sql = "SELECT $colsStr FROM $tableName"
        
        val filters = _state.value.filters
        if (filters.isNotEmpty()) {
            val whereClauses = filters.map { filter ->
                val sqlOp = filter.operator.sql
                val value = when (filter.operator) {
                    DriftFilterOperator.Contains -> "'%${filter.value}%'"
                    DriftFilterOperator.IsNo -> "0"
                    DriftFilterOperator.IsYes -> "1"
                    else -> {
                        // 根据类型决定是否加引号
                        val needsQuotes = filter.columnType.uppercase().let { 
                            it.contains("TEXT") || it.contains("DATE") || it.contains("STRING") 
                        }
                        if (needsQuotes) "'${filter.value}'" else filter.value
                    }
                }
                "${filter.columnName} $sqlOp $value"
            }
            if (whereClauses.isNotEmpty()) {
                sql += " WHERE ${whereClauses.joinToString(" AND ")}"
            }
        }

        val orderBy = _state.value.orderBy
        if (orderBy.isNotEmpty()) {
            val orderClauses = orderBy.joinToString(", ") { "${it.columnName} ${if (it.isAscending) "ASC" else "DESC"}" }
            sql += " ORDER BY $orderClauses"
        }
        
        if (limit > 0) sql += " LIMIT $limit"
        
        executeQuery(db.id, sql, "select")
    }

    /**
     * 切换列选择
     */
    fun toggleColumn(columnName: String) {
        val current = _state.value.selectedColumns
        val next = if (current.contains(columnName)) {
            current - columnName
        } else {
            current + columnName
        }
        _state.value = _state.value.copy(selectedColumns = next)
        
        // 自动重新查询
        _state.value.selectedTable?.let { table ->
            launch { executeSelect(table.name) }
        }
    }

    /**
     * 添加筛选条件
     */
    fun addFilter(filter: DriftFilter) {
        _state.value = _state.value.copy(filters = _state.value.filters + filter)
        applyFilters()
    }

    /**
     * 移除筛选条件
     */
    fun removeFilter(filter: DriftFilter) {
        _state.value = _state.value.copy(filters = _state.value.filters - filter)
        applyFilters()
    }

    /**
     * 清空筛选条件
     */
    fun clearFilters() {
        _state.value = _state.value.copy(filters = emptyList())
        applyFilters()
    }

    /**
     * 切换排序
     */
    fun toggleOrderBy(columnName: String) {
        val current = _state.value.orderBy
        val existing = current.find { it.columnName == columnName }
        val next = when {
            existing == null -> current + DriftOrderBy(columnName, true)
            existing.isAscending -> current.filter { it.columnName != columnName } + DriftOrderBy(columnName, false)
            else -> current.filter { it.columnName != columnName }
        }
        _state.value = _state.value.copy(orderBy = next)
        applyFilters()
    }

    /**
     * 清空排序
     */
    fun clearOrderBy() {
        _state.value = _state.value.copy(orderBy = emptyList())
        applyFilters()
    }

    /**
     * 应用筛选并查询
     */
    fun applyFilters() {
        _state.value.selectedTable?.let { table ->
            launch { executeSelect(table.name) }
        }
    }

    /**
     * 更新限制数量
     */
    fun updateLimit(limit: Int) {
        _state.value = _state.value.copy(limit = limit)
        _state.value.selectedTable?.let { table ->
            launch { executeSelect(table.name) }
        }
    }

    /**
     * 更新单元格数据
     */
    suspend fun updateCellValue(tableName: String, primaryKeyName: String, primaryKeyValue: Any, columnName: String, newValue: String) {
        val db = _state.value.selectedDatabase ?: return
        val formattedKey = if (primaryKeyValue is String) "'$primaryKeyValue'" else primaryKeyValue
        val formattedValue = if (newValue == "NULL") "NULL" else "'$newValue'"
        val sql = "UPDATE $tableName SET $columnName = $formattedValue WHERE $primaryKeyName = $formattedKey"
        executeQuery(db.id, sql, "update")
        executeSelect(tableName) // 刷新数据
    }

    /**
     * 执行 SQL 查询
     */
    suspend fun executeQuery(dbId: Int, sql: String, action: String = "execute-query", args: JsonArray = JsonArray()) {
        _state.value = _state.value.copy(queryResult = DriftAsyncState.Loading)
        try {
            val isolateId = getMainIsolateId()
            
            // 构造 ExecuteQuery 协议消息
            // [tag, method, sql, args, executorId]
            // tag: _tag_ExecuteQuery = 3
            // method index: select=3, insert=2, deleteOrUpdate=1, custom=0
            val trimmedSql = sql.trim().lowercase()
            val isSelect = action == "select" || trimmedSql.startsWith("select")
            
            val methodIndex = when {
                isSelect -> 3
                action == "insert" || trimmedSql.startsWith("insert") -> 2
                action == "delete" || action == "update" || trimmedSql.startsWith("delete") || trimmedSql.startsWith("update") -> 1
                else -> 0
            }
            
            val queryPayload = JsonArray().apply {
                add(3) // _tag_ExecuteQuery
                add(methodIndex)
                add(sql)
                add(args)
                add(JsonNull.INSTANCE) // executorId
            }
            
            val params = JsonObject().apply {
                addProperty("action", "execute-query")
                addProperty("db", dbId.toString())
                addProperty("query", queryPayload.toString())
            }
            
            val response = try {
                callServiceExtension("ext.drift.database", params)
            } catch (e: Throwable) {
                // 如果是 "Bad state: No element"，说明数据库 ID 失效，强制刷新列表
                if (e.message?.contains("No element") == true || e.message?.contains("Bad state") == true) {
                    addLog("Database ID $dbId invalid, refreshing list...")
                    fetchDatabases()
                }
                throw e
            }
            
            val rElement = response.get("r")
            
            val result = if (rElement == null || rElement.isJsonNull) {
                // 如果是 SELECT 但返回 null，可能是出错了；
                // 但如果是 INSERT/DELETE/UPDATE，Drift 协议目前在这种情况下 encodePayload 会返回 null
                if (isSelect) throw Exception("Select query returned null")
                DriftQueryResult(emptyList(), emptyList())
            } else if (rElement.isJsonArray) {
                decodeSelectResult(rElement.asJsonArray)
            } else {
                try {
                    decodeSelectResult(vmService.gson.fromJson(rElement.asString, JsonArray::class.java))
                } catch (e: Exception) {
                    if (isSelect) throw Exception("Unexpected result format: ${rElement.toString()}")
                    DriftQueryResult(emptyList(), emptyList())
                }
            }
            
            _state.value = _state.value.copy(queryResult = DriftAsyncState.Data(result))
            addLog("Query executed: $sql")
        } catch (e: Throwable) {
            _state.value = _state.value.copy(queryResult = DriftAsyncState.Error(e))
            addLog("Error executing query: ${e.message}")
        }
    }

    /**
     * 导出数据库
     * 返回 Base64 编码的字节数组
     */
    suspend fun exportDatabase(dbId: Int): ByteArray {
        val params = JsonObject().apply {
            addProperty("action", "download")
            addProperty("db", dbId.toString())
        }
        val response = callServiceExtension("ext.drift.database", params)
        val data = response.getAsJsonObject("r")?.get("data")?.asString 
            ?: throw Exception("No data returned from download action")
        
        return java.util.Base64.getDecoder().decode(data)
    }
    
    /**
     * 删除记录
     */
    suspend fun deleteData(tableName: String, primaryKeyName: String, primaryValue: Any) {
         val db = _state.value.selectedDatabase ?: return
         val sql = "DELETE FROM $tableName WHERE $primaryKeyName = ?"
         val args = JsonArray().apply {
             when (primaryValue) {
                 is Number -> add(primaryValue)
                 is Boolean -> add(primaryValue)
                 else -> add(primaryValue.toString())
             }
         }
         executeQuery(db.id, sql, "delete", args)
         // 刷新数据
         _state.value.selectedTable?.let { selectTable(it) }
    }

    /**
     * 辅助方法：Evaluate 表达式
     */
    private suspend fun evaluateExpression(isolateId: String, targetId: String, expression: String): com.google.gson.JsonElement {
        return suspendCancellableCoroutine { cont ->
            vmService.evaluate(isolateId, targetId, expression, object : EvaluateConsumer {
                override fun received(response: InstanceRef) {
                    if (response.getKind() == InstanceKind.String) {
                        launch {
                            try {
                                val fullString = if (response.getValueAsStringIsTruncated()) {
                                    fetchFullString(isolateId, response)
                                } else {
                                    response.getValueAsString() ?: ""
                                }
                                cont.resume(com.google.gson.JsonPrimitive(fullString))
                            } catch (e: Throwable) {
                                cont.resumeWithException(e)
                            }
                        }
                    } else {
                        cont.resume(com.google.gson.JsonPrimitive(response.getValueAsString() ?: ""))
                    }
                }
                
                override fun received(response: ErrorRef) {
                    cont.resumeWithException(Exception(response.getMessage()))
                }
                override fun received(response: Sentinel) {
                    cont.resumeWithException(Exception("Sentinel received"))
                }
                override fun onError(error: RPCError) {
                    cont.resumeWithException(error.exception)
                }
            })
        }
    }

    /**
     * 辅助方法：调用服务扩展
     */
    private suspend fun callServiceExtension(method: String, params: JsonObject): JsonObject {
        val isolateId = getMainIsolateId()
        return suspendCancellableCoroutine { cont ->
            vmService.callServiceExtension(isolateId, method, params, object : ServiceExtensionConsumer {
                override fun received(response: JsonObject) {
                    cont.resume(response)
                }
                override fun onError(error: RPCError) {
                    cont.resumeWithException(error.exception)
                }
            })
        }
    }

    /**
     * 解码 SelectResult (DriftProtocol 格式)
     */
    private fun decodeSelectResult(json: JsonArray): DriftQueryResult {
        if (json.size() == 0) return DriftQueryResult(emptyList(), emptyList())
        val tag = try { json.get(0).asInt } catch (e: Exception) { -1 }
        if (tag != 11) { // _tag_SelectResult = 11
            return DriftQueryResult(emptyList(), emptyList())
        }
        
        if (json.size() < 3) return DriftQueryResult(emptyList(), emptyList())
        
        val columnCount = json.get(1).asInt
        val columns = mutableListOf<String>()
        for (i in 0 until columnCount) {
            columns.add(json.get(2 + i).asString)
        }
        
        val rowCountIndex = 2 + columnCount
        if (json.size() <= rowCountIndex) return DriftQueryResult(columns, emptyList())
        
        val rowCount = json.get(rowCountIndex).asInt
        val rows = mutableListOf<DriftRow>()
        val dataOffset = rowCountIndex + 1
        for (i in 0 until rowCount) {
            val rowData = mutableMapOf<String, Any?>()
            for (c in 0 until columnCount) {
                val valIndex = dataOffset + i * columnCount + c
                if (valIndex < json.size()) {
                    val value = json.get(valIndex)
                    rowData[columns[c]] = if (value.isJsonNull) null else value.asString // 简单处理为 String
                }
            }
            rows.add(DriftRow(rowData))
        }
        
        return DriftQueryResult(columns, rows)
    }
    
    // 过渡方法，保持兼容性
    private fun decodeSelectResult(payload: String): DriftQueryResult {
        val json = try { 
            vmService.gson.fromJson(payload, JsonArray::class.java) 
        } catch (e: Exception) { return DriftQueryResult(emptyList(), emptyList()) }
        return decodeSelectResult(json)
    }

    private fun addLog(message: String) {
        _state.value = _state.value.copy(logs = _state.value.logs + message)
    }

    /**
     * 循环获取完整的字符串内容 (处理大对象截断)
     */
    private suspend fun fetchFullString(isolateId: String, ref: InstanceRef): String {
        val totalLength = ref.getLength()
        if (totalLength <= 0) return ref.getValueAsString() ?: ""
        
        val sb = StringBuilder()
        var offset = 0
        val chunkSize = 2000 // 每次获取 2000 字符
        
        while (offset < totalLength) {
            val count = if (offset + chunkSize > totalLength) totalLength - offset else chunkSize
            val instance = getObjectPart(isolateId, ref.getId(), offset, count)
            sb.append(instance.getValueAsString() ?: "")
            offset += count
            if ((instance.getValueAsString()?.length ?: 0) == 0) break // 防止死循环
        }
        return sb.toString()
    }

    private suspend fun getObjectPart(isolateId: String, objectId: String, offset: Int, count: Int): Instance {
        return suspendCancellableCoroutine { cont ->
            vmService.getObject(isolateId, objectId, offset, count, object : GetObjectConsumer {
                override fun received(response: Instance) { cont.resume(response) }
                override fun received(response: Breakpoint) { cont.resumeWithException(Exception("Unexpected Breakpoint")) }
                override fun received(response: ClassObj) { cont.resumeWithException(Exception("Unexpected ClassObj")) }
                override fun received(response: Code) { cont.resumeWithException(Exception("Unexpected Code")) }
                override fun received(response: Context) { cont.resumeWithException(Exception("Unexpected Context")) }
                override fun received(response: ErrorObj) { cont.resumeWithException(Exception("Unexpected ErrorObj")) }
                override fun received(response: Field) { cont.resumeWithException(Exception("Unexpected Field")) }
                override fun received(response: Func) { cont.resumeWithException(Exception("Unexpected Func")) }
                override fun received(response: Library) { cont.resumeWithException(Exception("Unexpected Library")) }
                override fun received(response: Null) { cont.resumeWithException(Exception("Unexpected Null")) }
                override fun received(response: Obj) { cont.resumeWithException(Exception("Unexpected Obj")) }
                override fun received(response: Script) { cont.resumeWithException(Exception("Unexpected Script")) }
                override fun received(response: Sentinel) { cont.resumeWithException(Exception("Unexpected Sentinel")) }
                override fun received(response: TypeArguments) { cont.resumeWithException(Exception("Unexpected TypeArguments")) }
                override fun onError(error: RPCError) { cont.resumeWithException(error.exception) }
            })
        }
    }

    fun dispose() {
        vmService.removeVmServiceListener(this)
        job.cancel()
    }
}
