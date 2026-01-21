package vm.drift

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vm.VmService
import vm.consumer.EvaluateConsumer
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
    val logs: List<String> = emptyList()
)

/**
 * Drift 服务类
 */
class DriftServices(val vmService: VmService) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val _state = MutableStateFlow(DriftState())
    val state: StateFlow<DriftState> = _state.asStateFlow()

    private var driftLibraryId: String? = null

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
        } catch (e: Exception) {
            _state.value = _state.value.copy(databases = DriftAsyncState.Error(e))
            addLog("Error fetching databases: ${e.message}")
        }
    }

    /**
     * 选择数据库
     */
    fun selectDatabase(db: DriftDatabase) {
        _state.value = _state.value.copy(selectedDatabase = db, selectedTable = null, queryResult = null)
    }

    /**
     * 选择表并加载数据
     */
    suspend fun selectTable(table: DriftTable) {
        _state.value = _state.value.copy(selectedTable = table)
        executeSelect(table.name)
    }

    /**
     * 执行简单的 SELECT *
     */
    suspend fun executeSelect(tableName: String, where: String? = null, orderBy: String? = null, limit: Int? = 100) {
        val db = _state.value.selectedDatabase ?: return
        var sql = "SELECT * FROM $tableName"
        if (!where.isNullOrBlank()) sql += " WHERE $where"
        if (!orderBy.isNullOrBlank()) sql += " ORDER BY $orderBy"
        if (limit != null) sql += " LIMIT $limit"
        
        executeQuery(db.id, sql, "select")
    }

    /**
     * 执行 SQL 查询
     */
    suspend fun executeQuery(dbId: Int, sql: String, action: String = "execute-query") {
        _state.value = _state.value.copy(queryResult = DriftAsyncState.Loading)
        try {
            val isolateId = getMainIsolateId()
            
            // 构造 ExecuteQuery 协议消息
            // [tag, method, sql, args, executorId]
            // tag: _tag_ExecuteQuery = 3
            // method index: select=3, insert=2, deleteOrUpdate=1, custom=0
            val methodIndex = when(action) {
                "select" -> 3
                "insert" -> 2
                "delete" -> 1
                "update" -> 1
                else -> 0
            }
            
            val queryPayload = JsonArray().apply {
                add(3) // _tag_ExecuteQuery
                add(methodIndex)
                add(sql)
                add(JsonArray()) // args
                add(JsonNull.INSTANCE) // executorId
            }
            
            val params = JsonObject().apply {
                addProperty("action", "execute-query")
                addProperty("db", dbId.toString())
                addProperty("query", queryPayload.toString())
            }
            
            val response = callServiceExtension("ext.drift.database", params)
            val resultPayload = response.get("r")?.asString ?: throw Exception("No result in response")
            
            // 解析 SelectResult
            // [_tag_SelectResult, columnCount, ...columns, rowCount, ...data]
            val result = decodeSelectResult(resultPayload)
            _state.value = _state.value.copy(queryResult = DriftAsyncState.Data(result))
            addLog("Query executed: $sql")
        } catch (e: Exception) {
            _state.value = _state.value.copy(queryResult = DriftAsyncState.Error(e))
            addLog("Error executing query: ${e.message}")
        }
    }
    
    /**
     * 删除记录
     */
    suspend fun deleteData(tableName: String, primaryKeyName: String, primaryKeyValue: Any) {
         val db = _state.value.selectedDatabase ?: return
         val sql = "DELETE FROM $tableName WHERE $primaryKeyName = $primaryKeyValue"
         executeQuery(db.id, sql, "delete")
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
                         cont.resume(com.google.gson.JsonPrimitive(response.getValueAsString()))
                    } else if (response.getKind() == InstanceKind.List || response.getKind() == InstanceKind.Map) {
                        // 如果是复杂对象，可能需要进一步 getObject，这里简化处理，假设 evaluate 返回的是简单的 json 可序列化对象映射
                        // 实际上 evaluate 返回的是 InstanceRef。如果我们在 Dart 端用了 json.encode，我们可以直接拿到 String
                        // 或者我们可以使用 evaluate 获取到的 id 再次调用 getObject (如果需要)
                        // 这里我们假设我们在 Dart 端使用了 json.encode 封装
                        cont.resume(com.google.gson.JsonPrimitive(response.getValueAsString()))
                    } else {
                        // 尝试从返回的 response 对应的 id 获取对象，或者直接使用 valueAsString
                        cont.resume(com.google.gson.JsonPrimitive(response.getValueAsString() ?: ""))
                    }
                }
                
                // 处理 Evaluate 返回 JSON 字符串的情况 (我们需要在 Dart 端包裹一下)
                // 或者我们可以直接处理 InstanceRef 到 JsonElement 的转换，但比较复杂。
                // 推荐在 Dart 端调用 describe() 返回 JSON String
                
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
        return suspendCancellableCoroutine { cont ->
            launch {
                val isolateId = getMainIsolateId()
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
    }

    /**
     * 解码 SelectResult (DriftProtocol 格式)
     */
    private fun decodeSelectResult(payload: String): DriftQueryResult {
        val json = vmService.gson.fromJson(payload, JsonArray::class.java)
        val tag = json.get(0).asInt
        if (tag != 11) { // _tag_SelectResult = 11
            return DriftQueryResult(emptyList(), emptyList())
        }
        
        if (json.size() == 1) return DriftQueryResult(emptyList(), emptyList())
        
        val columnCount = json.get(1).asInt
        val columns = mutableListOf<String>()
        for (i in 0 until columnCount) {
            columns.add(json.get(2 + i).asString)
        }
        
        val rowCount = json.get(2 + columnCount).asInt
        val rows = mutableListOf<DriftRow>()
        val dataOffset = 3 + columnCount
        for (i in 0 until rowCount) {
            val rowData = mutableMapOf<String, Any?>()
            for (c in 0 until columnCount) {
                val value = json.get(dataOffset + i * columnCount + c)
                rowData[columns[c]] = if (value.isJsonNull) null else value.asString // 简单处理为 String
            }
            rows.add(DriftRow(rowData))
        }
        
        return DriftQueryResult(columns, rows)
    }

    private fun addLog(message: String) {
        _state.value = _state.value.copy(logs = _state.value.logs + message)
    }

    fun dispose() {
        job.cancel()
    }
}
