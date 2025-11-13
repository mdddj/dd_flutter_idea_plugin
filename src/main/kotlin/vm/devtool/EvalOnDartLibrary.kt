package vm.devtool


import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vm.*
import vm.consumer.EvaluateConsumer
import vm.consumer.GetInstanceConsumer
import vm.element.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * 当请求在完成前被取消时抛出。
 */
class CancelledException : Exception("The request was cancelled before it could be completed.")

/**
 * 当库未在当前 isolate 中找到时抛出。
 */
class LibraryNotFoundException(libraryName: String) :
    Exception("Library '$libraryName' not found in the current isolate.")

/**
 * 当 `evaluate` RPC 返回一个错误引用时抛出。
 */
class EvalErrorException(
    expression: String,
    errorRef: ErrorRef,
    val scope: Map<String, String>? = null
) : Exception("Evaluation of '$expression' failed with error: ${errorRef.getMessage()}")

/**
 * 当 `evaluate` RPC 返回一个 Sentinel 时抛出。
 */
class EvalSentinelException(
    expression: String,
    val sentinel: Sentinel,
    val scope: Map<String, String>? = null
) : Exception("Evaluation of '$expression' returned a sentinel: ${sentinel.valueAsString}")


/**
 * 一个工具类，用于在特定 Dart 库的上下文中执行代码。
 * 模仿 Flutter DevTools 中的同名类。
 *
 * @param libraryUri 要执行代码的目标库的 URI, e.g., "package:provider/provider.dart"。
 * @param vmService VmService 实例。
 */
class EvalOnDartLibrary(
    private val libraryUri: String,
    private val vmService: VmService,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var libraryRef: LibraryRef? = null
    private var initializeJob: Deferred<LibraryRef?>? = null
    private val mutex = Mutex()

    /**
     * 初始化并获取目标库的引用。
     * 这个方法是线程安全的，并且只会执行一次初始化。
     * @param isolateId 目标 isolate 的 ID。
     * @return 目标库的 [LibraryRef]。
     * @throws LibraryNotFoundException 如果找不到指定的库。
     */
    private suspend fun initializeAndGetLibraryRef(isolateId: String): LibraryRef {
        mutex.withLock {
            if (libraryRef != null) {
                return libraryRef!!
            }
            val job = initializeJob ?: scope.async {
                val vm = vmService.getVm()
                val isolate = vm.getIsolates().find { it.getId() == isolateId }
                    ?: throw IllegalStateException("Isolate with ID '$isolateId' not found.")
                val fullIsolate = vmService.getIsolateByIdPub(isolate.getId()!!)
                    ?: throw IllegalStateException("Could not fetch full details for isolate '$isolateId'.")
                val foundLibrary = fullIsolate.getLibraries().find { it.getUri() == libraryUri }
                if (foundLibrary != null) {
                    libraryRef = foundLibrary
                    foundLibrary
                } else {
                    throw LibraryNotFoundException(libraryUri)
                }
            }.also { initializeJob = it }

            return job.await() as LibraryRef
        }
    }

    /**
     * 在目标库的上下文中安全地执行一个 Dart 表达式。
     * 这个方法会处理 RPC 错误和 Sentinel，并以异常形式抛出。
     *
     * @param isolateId 目标 isolate 的 ID。
     * @param expression 要执行的 Dart 表达式。
     * @param scope  一个变量名到对象ID的映射，用于表达式的作用域。
     * @return 表达式结果的 [InstanceRef]。
     * @throws CancelledException 如果当前协程被取消。
     * @throws LibraryNotFoundException 如果找不到目标库。
     * @throws EvalErrorException 如果评估返回错误。
     * @throws EvalSentinelException 如果评估返回哨兵值。
     */
    suspend fun safeEval(
        isolateId: String,
        expression: String,
        scope: Map<String, String>? = null,
        disableBreakpoints: Boolean = true
    ): InstanceRef = coroutineScope {
        val lib = initializeAndGetLibraryRef(isolateId)

        suspendCancellableCoroutine { continuation ->
            vmService.evaluate(
                isolateId,
                lib.getId(),
                expression,
                scope,
                disableBreakpoints,
                object : EvaluateConsumer {
                    override fun received(response: InstanceRef) {
                        if (continuation.isActive) continuation.resume(response)
                    }

                    override fun received(response: ErrorRef) {
                        if (continuation.isActive) continuation.resumeWithException(
                            EvalErrorException(expression, response, scope)
                        )
                    }

                    override fun received(response: Sentinel) {
                        if (continuation.isActive) continuation.resumeWithException(
                            EvalSentinelException(expression, response, scope)
                        )
                    }


                    override fun onError(error: RPCError) {
                        if (continuation.isActive) continuation.resumeWithException(error.exception)
                    }
                })
        }
    }

    /**
     * 获取一个对象的完整 [Instance] 描述。
     * @param isolateId 目标 isolate 的 ID。
     * @param instanceRef 要获取详情的对象的引用。
     * @return 完整的 [Instance] 对象。
     * @throws DartVMRPCException 如果对象已过期（Sentinel）
     */
    suspend fun getInstance(isolateId: String, instanceRef: InstanceRef): Instance {
        return suspendCancellableCoroutine { continuation ->
            vmService.getInstance(isolateId, instanceRef.getId(),object : GetInstanceConsumer{
                override fun received(response: Instance) {
                    continuation.resume(response)
                }

                override fun onError(error: RPCError) {
                    // 检查是否是 Sentinel 错误
                    val exception = error.exception
                    if (exception.message?.contains("Sentinel") == true || 
                        exception.message?.contains("Expired") == true) {
                        // 创建一个特殊的 Sentinel Instance 来表示过期的对象
                        val sentinelInstance = createSentinelInstance(instanceRef.getId())
                        continuation.resume(sentinelInstance)
                    } else {
                        continuation.resumeWithException(exception)
                    }
                }

            })
        }
    }
    
    /**
     * 创建一个表示 Sentinel 的 Instance 对象
     */
    private fun createSentinelInstance(id: String): Instance {
        val json = com.google.gson.JsonObject().apply {
            addProperty("type", "Instance")
            addProperty("id", id)
            addProperty("kind", "Null")
            addProperty("valueAsString", "<expired>")
            add("class", com.google.gson.JsonObject().apply {
                addProperty("type", "@Class")
                addProperty("id", "classes/0")
                addProperty("name", "Expired")
            })
        }
        return Instance(json)
    }


    suspend fun getClassObject(isolateId: String, objectId: String): ClassObj? {
        return vmService.getObjectWithClassObj(isolateId, objectId)
    }

    suspend fun getFieldObject(isolateId: String, objectId: String): Field? {
        return vmService.getObjectWithField(isolateId, objectId)
    }

    suspend fun getObjectWithLibrary(isolateId: String,objectId: String): Library? = vmService.getObjectWithLibrary(isolateId,objectId)
}