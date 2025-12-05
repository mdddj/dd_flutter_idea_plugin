package vm.sp

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vm.VmService
import vm.consumer.EvaluateConsumer
import vm.element.*
import vm.getVm
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SharedPreferences 数据类型
 */
sealed class SharedPreferencesData {
    abstract val value: Any
    abstract val kind: String
    abstract val valueAsString: String

    data class StringData(override val value: String) : SharedPreferencesData() {
        override val kind = "String"
        override val valueAsString = value
    }

    data class IntData(override val value: Int) : SharedPreferencesData() {
        override val kind = "int"
        override val valueAsString = value.toString()
    }

    data class DoubleData(override val value: Double) : SharedPreferencesData() {
        override val kind = "double"
        override val valueAsString = value.toString()
    }

    data class BoolData(override val value: Boolean) : SharedPreferencesData() {
        override val kind = "bool"
        override val valueAsString = value.toString()
    }

    data class StringListData(override val value: List<String>) : SharedPreferencesData() {
        override val kind = "List<String>"
        override val valueAsString: String
            get() = value.mapIndexed { index, s -> "$index -> $s" }.joinToString("\n")
    }

    companion object {
        fun fromKindAndValue(kind: String?, value: Any?): SharedPreferencesData? {
            return when (kind) {
                "int" -> IntData((value as? Number)?.toInt() ?: return null)
                "bool" -> BoolData(value as? Boolean ?: return null)
                "double" -> DoubleData((value as? Number)?.toDouble() ?: return null)
                "String" -> StringData(value as? String ?: return null)
                else -> {
                    if (kind?.contains("List") == true) {
                        @Suppress("UNCHECKED_CAST")
                        StringListData((value as? List<*>)?.filterIsInstance<String>() ?: return null)
                    } else null
                }
            }
        }
    }
}


/**
 * 异步状态封装
 */
sealed class AsyncState<out T> {
    data object Loading : AsyncState<Nothing>()
    data class Data<T>(val data: T) : AsyncState<T>()
    data class Error(val error: Throwable) : AsyncState<Nothing>()

    fun dataOrNull(): T? = (this as? Data)?.data
}

/**
 * 选中的 SharedPreferences Key
 */
data class SelectedKey(
    val key: String,
    val value: AsyncState<SharedPreferencesData>
)

/**
 * SharedPreferences 状态
 */
data class SharedPreferencesState(
    val allKeys: AsyncState<List<String>> = AsyncState.Loading,
    val selectedKey: SelectedKey? = null,
    val editing: Boolean = false,
    val legacyApi: Boolean = false
)

/**
 * SharedPreferences 服务类
 * 通过 Dart VM Service 与 Flutter 应用的 SharedPreferences 进行交互
 */
class SharedPreferencesServices(val vmService: VmService) : CoroutineScope {
    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val _state = MutableStateFlow(SharedPreferencesState())
    val state: StateFlow<SharedPreferencesState> = _state.asStateFlow()

    private var asyncKeys: List<String> = emptyList()
    private var legacyKeys: List<String> = emptyList()

    private val currentKeys: List<String>
        get() = if (_state.value.legacyApi) legacyKeys else asyncKeys

    // 缓存 shared_preferences library 的引用
    private var sharedPreferencesLibraryId: String? = null

    /**
     * 获取主 Isolate ID
     */
    private suspend fun getMainIsolateId(): String {
        val vm = vmService.getVm()
        val mainIsolate = vm.getIsolates().find { it.getName() == "main" }
        return mainIsolate?.getId() ?: throw IllegalStateException("Main isolate not found")
    }

    /**
     * 获取 shared_preferences DevTools 扩展库的 ID
     * SharedPreferencesDevToolsExtensionData 类定义在 shared_preferences_devtools_extension_data.dart 中
     */
    private suspend fun getSharedPreferencesLibraryId(isolateId: String): String {
        // 如果已缓存，直接返回
        sharedPreferencesLibraryId?.let { return it }

        val isolate = vmService.getIsolateByIdPub(isolateId)
            ?: throw IllegalStateException("Could not get isolate details")

        // 查找 shared_preferences_devtools_extension_data.dart 库
        // 这个文件包含 SharedPreferencesDevToolsExtensionData 类
        val libraries = isolate.getLibraries()
        val spLibrary = libraries.find { lib ->
            val uri = lib.getUri() ?: ""
            uri.contains("shared_preferences") && uri.contains("devtools_extension_data")
        } ?: throw IllegalStateException(
            "shared_preferences DevTools extension not found. " +
                    "Make sure your Flutter app uses shared_preferences >= 2.3.0 and has called SharedPreferences.getInstance() at least once."
        )

        val libraryId = spLibrary.getId()
            ?: throw IllegalStateException("Library ID is null")

        sharedPreferencesLibraryId = libraryId
        return libraryId
    }

    /**
     * 通过 evaluate 调用 Dart 代码并监听扩展事件
     */
    private suspend fun evalMethod(
        method: String,
        eventKind: String
    ): JsonObject {
        return suspendCancellableCoroutine { cont ->
            launch {
                try {
                    val isolateId = getMainIsolateId()
                    val libraryId = getSharedPreferencesLibraryId(isolateId)

                    // 监听扩展事件
                    var received = false
                    val listener = object : VmService.VmEventListener {
                        override fun onVmEvent(streamId: String, event: Event) {
                            if (received) return
                            val extensionKind = event.getExtensionKind()
                            if (extensionKind == "shared_preferences.$eventKind") {
                                received = true
                                vmService.removeEventListener(this)
                                val data = event.getExtensionData()
                                if (data != null && cont.isActive) {
                                    cont.resume(data.json)
                                } else if (cont.isActive) {
                                    cont.resumeWithException(Exception("No extension data received"))
                                }
                            }
                        }
                    }
                    vmService.addEventListener(listener)

                    // 执行 evaluate - 使用 libraryId 作为 targetId
                    val expression = "SharedPreferencesDevToolsExtensionData().$method"
                    vmService.evaluate(isolateId, libraryId, expression, object : EvaluateConsumer {
                        override fun received(response: ErrorRef) {
                            if (!received && cont.isActive) {
                                vmService.removeEventListener(listener)
                                cont.resumeWithException(Exception("Evaluate error: ${response.getMessage()}"))
                            }
                        }

                        override fun received(response: InstanceRef) {
                            // 等待扩展事件
                        }

                        override fun received(response: Sentinel) {
                            if (!received && cont.isActive) {
                                vmService.removeEventListener(listener)
                                cont.resumeWithException(Exception("Sentinel received"))
                            }
                        }

                        override fun onError(error: RPCError) {
                            if (!received && cont.isActive) {
                                vmService.removeEventListener(listener)
                                cont.resumeWithException(error.exception)
                            }
                        }
                    })

                    // 超时处理
                    delay(10000)
                    if (!received && cont.isActive) {
                        vmService.removeEventListener(listener)
                        cont.resumeWithException(Exception("Timeout waiting for event: $eventKind"))
                    }
                } catch (e: Exception) {
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }
            }
        }
    }


    /**
     * 获取所有 keys
     */
    suspend fun fetchAllKeys() {
        _state.value = _state.value.copy(
            allKeys = AsyncState.Loading,
            selectedKey = null
        )

        try {
            val data = evalMethod("requestAllKeys()", "all_keys")

            val asyncKeysArray = data.getAsJsonArray("asyncKeys")
            val legacyKeysArray = data.getAsJsonArray("legacyKeys")

            legacyKeys = legacyKeysArray?.map { it.asString } ?: emptyList()

            // 过滤掉重复的 legacy keys
            val legacyPrefix = "flutter."
            asyncKeys = asyncKeysArray?.map { it.asString }?.filter { key ->
                !(key.startsWith(legacyPrefix) && legacyKeys.contains(key.removePrefix(legacyPrefix)))
            } ?: emptyList()

            _state.value = _state.value.copy(
                allKeys = AsyncState.Data(currentKeys)
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                allKeys = AsyncState.Error(e)
            )
        }
    }

    /**
     * 选择一个 key 并获取其值
     */
    suspend fun selectKey(key: String) {
        stopEditing()

        _state.value = _state.value.copy(
            selectedKey = SelectedKey(key, AsyncState.Loading)
        )

        try {
            val legacy = _state.value.legacyApi
            val data = evalMethod("requestValue('$key', $legacy)", "value")

            val value = data.get("value")
            val kind = data.get("kind")?.asString

            val parsedValue: Any? = when {
                value == null -> null
                value.isJsonNull -> null
                value.isJsonPrimitive -> {
                    val primitive = value.asJsonPrimitive
                    when {
                        primitive.isBoolean -> primitive.asBoolean
                        primitive.isNumber -> {
                            if (kind == "int") primitive.asInt
                            else primitive.asDouble
                        }

                        primitive.isString -> primitive.asString
                        else -> null
                    }
                }

                value.isJsonArray -> value.asJsonArray.map { it.asString }
                else -> null
            }

            val spData = SharedPreferencesData.fromKindAndValue(kind, parsedValue)

            if (spData != null) {
                _state.value = _state.value.copy(
                    selectedKey = SelectedKey(key, AsyncState.Data(spData))
                )
            } else {
                _state.value = _state.value.copy(
                    selectedKey = SelectedKey(key, AsyncState.Error(Exception("Unsupported type: $kind")))
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                selectedKey = SelectedKey(key, AsyncState.Error(e))
            )
        }
    }

    /**
     * 修改值
     */
    suspend fun changeValue(key: String, newValue: SharedPreferencesData) {
        _state.value = _state.value.copy(
            selectedKey = _state.value.selectedKey?.copy(value = AsyncState.Loading)
        )

        try {
            val legacy = _state.value.legacyApi
            val serializedValue = when (newValue) {
                is SharedPreferencesData.StringData -> "\"${newValue.value.replace("\"", "\\\"")}\""
                is SharedPreferencesData.StringListData -> {
                    val jsonArray = JsonArray()
                    newValue.value.forEach { jsonArray.add(it) }
                    jsonArray.toString()
                }

                else -> newValue.value.toString()
            }

            evalMethod(
                "requestValueChange('$key', '$serializedValue', '${newValue.kind}', $legacy)",
                "change_value"
            )

            selectKey(key)
            stopEditing()
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                selectedKey = _state.value.selectedKey?.copy(value = AsyncState.Error(e))
            )
        }
    }

    /**
     * 删除 key
     */
    suspend fun deleteKey(key: String) {
        _state.value = _state.value.copy(
            allKeys = AsyncState.Loading,
            selectedKey = _state.value.selectedKey?.copy(value = AsyncState.Loading)
        )

        try {
            val legacy = _state.value.legacyApi
            evalMethod("requestRemoveKey('$key', $legacy)", "remove")
            fetchAllKeys()
            stopEditing()
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                allKeys = AsyncState.Error(e)
            )
        }
    }

    /**
     * 过滤 keys
     */
    fun filter(token: String) {
        val filtered = if (token.isBlank()) {
            currentKeys
        } else {
            currentKeys.filter { it.contains(token, ignoreCase = true) }
        }
        _state.value = _state.value.copy(
            allKeys = AsyncState.Data(filtered)
        )
    }

    /**
     * 切换 API 类型
     */
    fun selectApi(legacyApi: Boolean) {
        _state.value = _state.value.copy(
            legacyApi = legacyApi,
            allKeys = AsyncState.Data(if (legacyApi) legacyKeys else asyncKeys)
        )
    }

    /**
     * 开始编辑
     */
    fun startEditing() {
        _state.value = _state.value.copy(editing = true)
    }

    /**
     * 停止编辑
     */
    fun stopEditing() {
        _state.value = _state.value.copy(editing = false)
    }

    /**
     * 清理资源
     */
    fun dispose() {
        job.cancel()
    }
}
