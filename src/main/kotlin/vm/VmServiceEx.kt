package vm

import com.google.gson.JsonObject
import kotlinx.coroutines.suspendCancellableCoroutine
import vm.consumer.*
import vm.element.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


//获取 vm
suspend fun VmService.getVm(): VM {
    return suspendCoroutine { continuation ->
        getVM(object : VMConsumer {
            override fun received(response: VM) {
                continuation.resume(response)
            }

            override fun onError(error: RPCError) {
                continuation.resumeWithException(error.exception)
            }
        })
    }
}

suspend fun VmService.getVersion(): Version {
    return suspendCoroutine { continuation ->
        getVersion(object : VersionConsumer {
            override fun received(response: Version) {
                continuation.resume(response)
            }

            override fun onError(error: RPCError) {
                continuation.resumeWithException(error.exception)
            }
        })
    }
}




suspend fun VmService.getRootWidgetTree(
    isolateId: String,
    groupName: String,
    isSummaryTree: Boolean = true,
    withPreviews: Boolean = false,
    fullDetails: Boolean = false
): WidgetTreeResponse? {
    return suspendCoroutine { continuation ->
        try {
            val params = JsonObject()
            params.addProperty("isolateId", isolateId)
            params.addProperty("isSummaryTree", isSummaryTree)
            params.addProperty("withPreviews", withPreviews)
            params.addProperty("fullDetails", fullDetails)
            params.addProperty("groupName", groupName)

            callServiceExtension(
                isolateId,
                "ext.flutter.inspector.getRootWidgetTree",
                params,
                object : ServiceExtensionConsumer {
                    override fun received(result: JsonObject) {
                        try {
                            // 使用安全的Gson配置来处理深度嵌套
                            val response = SafeGsonConfig.gson.fromJson(result, WidgetTreeResponse::class.java)
                            continuation.resume(response)
                        } catch (e: Exception) {
                            println("使用安全Gson解析失败，尝试普通Gson: ${e.message}")
                            try {
                                // 回退到普通Gson
                                val response = gson.fromJson(result, WidgetTreeResponse::class.java)
                                continuation.resume(response)
                            } catch (fallbackException: Exception) {
                                continuation.resumeWithException(fallbackException)
                            }
                        }
                    }

                    override fun onError(error: RPCError) {
                        continuation.resumeWithException(error.exception)
                    }

                }
            )

        } catch (e: Exception) {
            println("获取 widget tree 失败: ${e.message}")
            continuation.resumeWithException(e)
        }
    }
}

suspend fun VmService.getDetailsSubtree(
    isolateId: String,
    groupName: String,
    diagnosticsNodeId: String
): JsonObject {
    val params = JsonObject()
    params.addProperty("isolateId", isolateId)
    params.addProperty("objectGroup", groupName)
    params.addProperty("arg", diagnosticsNodeId)
    return suspendCoroutine { continuation ->
        callServiceExtension(
            isolateId,
            "ext.flutter.inspector.getDetailsSubtree",
            params,
            object : ServiceExtensionConsumer {
                override fun received(result: JsonObject) {
                    continuation.resume(result)
                }

                override fun onError(error: RPCError) {
                    continuation.resumeWithException(error.exception)
                }

            }
        )
    }
}


suspend fun VmService.getProperties(
    isolateId: String,
    groupName: String,
    diagnosticsNodeId: String
): FlutterInspectorGetPropertiesResponse {
    val params = JsonObject().apply {
        addProperty("objectGroup", groupName)
        addProperty("arg", diagnosticsNodeId)
    }
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.flutter.inspector.getProperties",
            params = params,
            object : ServiceExtensionConsumer {
                override fun received(result: JsonObject) = cont.resume(
                    gson.fromJson(
                        result,
                        FlutterInspectorGetPropertiesResponse::class.java
                    )
                )

                override fun onError(error: RPCError) = cont.resumeWithException(error.exception)
            },
        )
    }
}

/**
 * 获取带有Text预览的Widget Tree
 * 使用保守的参数设置来避免过深的嵌套
 */
suspend fun VmService.getDetailedWidgetTree(
    isolateId: String,
    groupName: String
): WidgetTreeResponse? {
    return try {
        // 第一次尝试：启用预览但保持摘要模式
        getRootWidgetTree(
            isolateId = isolateId,
            groupName = groupName,
            isSummaryTree = true,   // 保持摘要模式
            withPreviews = true,    // 启用预览获取Text内容
            fullDetails = false     // 不启用完整详细信息
        )
    } catch (e: Exception) {
        println("获取带预览的Widget Tree失败: ${e.message}")
        try {
            // 第二次尝试：只使用基本参数
            getRootWidgetTree(
                isolateId = isolateId,
                groupName = groupName,
                isSummaryTree = true,
                withPreviews = false,
                fullDetails = false
            )
        } catch (fallbackException: Exception) {
            println("获取基本Widget Tree也失败: ${fallbackException.message}")
            null
        }
    }
}

/**
 * 限制Widget Tree的深度，避免过深嵌套
 */
fun WidgetTreeResponse.limitDepth(maxDepth: Int = 50): WidgetTreeResponse {
    return this.copy(result = this.result?.limitDepth(maxDepth, 0))
}

/**
 * 限制WidgetNode的深度
 */
fun WidgetNode.limitDepth(maxDepth: Int, currentDepth: Int): WidgetNode? {
    if (currentDepth >= maxDepth) {
        // 达到最大深度，返回一个简化的节点
        return this.copy(
            children = null,
            description = "${this.description} (深度限制)"
        )
    }

    return this.copy(
        children = this.children?.mapNotNull { child ->
            child.limitDepth(maxDepth, currentDepth + 1)
        }
    )
}

/**
 * 控制 Flutter Inspector 的 Overlay（在设备上渲染的调试图层）。
 *
 * @param isolateId    当前的 isolate，例如 `vmService.mainIsolates()?.getId()!!`
 * @param enabled      true 显示 inspector overlay，false 隐藏
 * @return             Boolean 是否设置成功（vm_service 返回值中 `"result": {"enabled": "true"}`)
 */
suspend fun VmService.setInspectorOverlay(
    isolateId: String,
    enabled: Boolean
): Boolean = suspendCoroutine { cont ->
    val params = JsonObject().apply {
        addProperty("enabled", enabled.toString())
    }
    callServiceExtension(
        isolateId = isolateId,
        method = "ext.flutter.inspector.show",
        params = params,
        consumer = object : ServiceExtensionConsumer {
            override fun received(result: JsonObject) {
                cont.resume(true)
            }

            override fun onError(error: RPCError) {
                cont.resumeWithException(error.exception)
            }
        }
    )
}

/**
 * 获取当前 Flutter Inspector Overlay 的状态
 * 通过调用inspector服务来检查overlay状态
 *
 * @param isolateId    当前的 isolate
 * @return             Boolean 当前是否启用了 inspector overlay
 */
suspend fun VmService.getInspectorOverlayState(
    isolateId: String
): Boolean = suspendCoroutine { cont ->
    val params = JsonObject()

    // 尝试调用inspector.show方法来获取当前状态
    callServiceExtension(
        isolateId = isolateId,
        method = "ext.flutter.inspector.show",
        params = params,
        consumer = object : ServiceExtensionConsumer {
            override fun received(result: JsonObject) {
                try {
                    // 检查返回结果中的enabled状态
                    val enabled = result.get("enabled")?.asString == "true"
                    cont.resume(enabled)
                } catch (e: Exception) {
                    // 如果解析失败，尝试检查widget tree是否就绪
                    checkWidgetTreeReady(isolateId, cont)
                }
            }

            override fun onError(error: RPCError) {
                // 如果调用失败，尝试其他方法
                checkWidgetTreeReady(isolateId, cont)
            }
        }
    )
}

/**
 * 备用方法：检查Widget Tree是否就绪,
 */
fun VmService.checkWidgetTreeReady(
    isolateId: String,
    cont: kotlin.coroutines.Continuation<Boolean>
) {
    val params = JsonObject()
    callServiceExtension(
        isolateId = isolateId,
        method = "ext.flutter.inspector.isWidgetTreeReady",
        params = params,
        consumer = object : ServiceExtensionConsumer {
            override fun received(result: JsonObject) {
                try {
                    val isReady = result.get("result")?.asString == "true"
                    cont.resume(isReady)
                } catch (e: Exception) {
                    cont.resume(false)
                }
            }

            override fun onError(error: RPCError) {
                cont.resume(false)
            }
        }
    )
}

/**
 * 获取当前选中的Widget信息
 *
 * @param isolateId 当前的isolate
 * @param groupName 对象组名称
 * @return 选中的Widget详细信息
 */
suspend fun VmService.getSelectedWidget(
    isolateId: String,
    groupName: String
): SelectedWidgetInfo? = suspendCoroutine { cont ->
    val params = JsonObject().apply {
        addProperty("objectGroup", groupName)
    }

    callServiceExtension(
        isolateId = isolateId,
        method = "ext.flutter.inspector.getSelectedRenderObject",
        params = params,
        consumer = object : ServiceExtensionConsumer {
            override fun received(result: JsonObject) {
                try {
                    val selectedWidget = SafeGsonConfig.gson.fromJson(result, SelectedWidgetInfo::class.java)
                    cont.resume(selectedWidget)
                } catch (e: Exception) {
                    println("解析选中Widget信息失败: ${e.message}")
                    cont.resume(null)
                }
            }

            override fun onError(error: RPCError) {
                cont.resume(null)
            }
        }
    )
}

//ext.dart.io.socketProfilingEnabled
fun VmService.socketProfilingEnable(isolateId: String,enabled: Boolean) {
    callServiceExtension(
        isolateId = isolateId,
        method = "ext.dart.io.socketProfilingEnabled",
        params = JsonObject().apply {
            addProperty("enabled", enabled)
        },
        defaultServiceExtensionConsumer({
            println("成功:${it}")
        }){
            println("失败:$it")
        }
    )
}


suspend fun VmService.getHttpProfile(isolateId: String,updatedSince: Long? = null) : JsonObject? {
    val isAvailable = isHttpProfilingAvailable(isolateId)
    if(!isAvailable){
        println("不可用。。")
    }
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getHttpProfile",
            params = JsonObject().apply {
                updatedSince?.let {
                    addProperty("updatedSince", it)
                }
            },
            object : ServiceExtensionConsumer {
                override fun received(result: JsonObject) {
                    cont.resume(result)
                }

                override fun onError(error: RPCError) {
                    cont.resume(null)
                }

            }
        )
    }
}



suspend fun VmService.isHttpProfilingAvailable(isolateId: String): Boolean {
    return getIsolateById(isolateId)?.getExtensionRPCs()?.contains("ext.dart.io.getHttpProfile") ?: false
}



/**
 * 检索 [stringRef] 的完整字符串值。
 *
 * 如果 [stringRef] 中存储的字符串值未被截断，则直接返回该值。
 * 如果值被截断，则会发起一个额外的 getObject 调用以获取完整值。
 *
 * @param isolateId 目标 isolate 的 ID.
 * @param stringRef 字符串的引用 [InstanceRef].
 * @return 完整的字符串值。如果对象已过期且无法检索到完整字符串，则抛出 [IllegalStateException]。
 * @throws RPCError 如果 VM 服务调用失败。
 * @throws IllegalStateException 如果对象已过期且无法检索到完整字符串。
 */
suspend fun VmService.retrieveFullStringValue(
    isolateId: String,
    stringRef: InstanceRef
): String? {
    // 1. 检查字符串是否被截断
    if (!stringRef.getValueAsStringIsTruncated()) {
        return stringRef.getValueAsString()
    }
    return suspendCancellableCoroutine { continuation ->
        val objectId = stringRef.getId()
        val length = stringRef.getLength()

        if (objectId == null) {
            continuation.resumeWithException(
                IllegalArgumentException("InstanceRef must have a valid id and length for truncation retrieval.")
            )
            return@suspendCancellableCoroutine
        }

        getObject(isolateId, objectId, 0, length, object : GetObjectConsumer {
            override fun received(response: Breakpoint) {
                continuation.resume(null)
            }

            override fun received(response: ClassObj) {
                continuation.resume(null)
            }

            override fun received(response: Code) {
                continuation.resume(null)
            }

            override fun received(response: Context) {
                continuation.resume(null)
            }

            override fun received(response: ErrorObj) {
                continuation.resume(null)
            }

            override fun received(response: Field) {
                continuation.resume(null)
            }

            override fun received(response: Func) {
                continuation.resume(null)
            }

            override fun received(response: Instance) {
                continuation.resume(null)
            }

            override fun received(response: Library) {
                continuation.resume(null)
            }

            override fun received(response: Null) {
                continuation.resume(null)
            }

            override fun received(response: Obj) {
                if (response is Instance) {
                    if (continuation.isActive) {
                        continuation.resume(response.getValueAsString())
                    }
                } else {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("Expected an Instance object but got ${response.type} for string retrieval.")
                        )
                    }
                }
            }

            override fun received(response: Script) {
                TODO("Not yet implemented")
            }

            override fun received(response: Sentinel) {
                // 4. 如果返回 Sentinel，说明对象已过期，无法获取
                if (continuation.isActive) {
                    val truncatedValue = stringRef.getValueAsString()
                    continuation.resumeWithException(
                        IllegalStateException("The full string for \"$truncatedValue...\" is unavailable (expired).")
                    )
                }
            }

            override fun received(response: TypeArguments) {
                TODO("Not yet implemented")
            }

            override fun onError(error: RPCError) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error.exception)
                }
            }
        })

    }
}


// 获取 provider ids
suspend fun VmService.getObject(isolateId: String,instanceId: String): Instance? {
    return suspendCancellableCoroutine { continuation ->
        getObject(isolateId,instanceId,object : GetObjectConsumer{
            override fun received(response: Breakpoint) {
                continuation.resume(null)
            }

            override fun received(response: ClassObj) {
                continuation.resume(null)
            }

            override fun received(response: Code) {
                continuation.resume(null)
            }

            override fun received(response: Context) {
                continuation.resume(null)
            }

            override fun received(response: ErrorObj) {
                continuation.resume(null)
            }

            override fun received(response: Field) {
                continuation.resume(null)
            }

            override fun received(response: Func) {
                continuation.resume(null)
            }

            override fun received(response: Instance) {
                continuation.resume(response)
            }

            override fun received(response: Library) {
                continuation.resume(null)
            }

            override fun received(response: Null) {
                continuation.resume(null)
            }

            override fun received(response: Obj) {
                continuation.resume(null)
            }

            override fun received(response: Script) {
                continuation.resume(null)
            }

            override fun received(response: Sentinel) {
                continuation.resume(null)
            }

            override fun received(response: TypeArguments) {
                continuation.resume(null)
            }

            override fun onError(error: RPCError) {
                continuation.resume(null)
            }

        })
    }
}

suspend fun VmService.getObjectWithClassObj(isolateId: String, instanceId: String): ClassObj? {
    return suspendCancellableCoroutine { continuation ->
        getObject(isolateId,instanceId,object : GetObjectConsumer{
            override fun received(response: Breakpoint) {
                continuation.resume(null)
            }

            override fun received(response: ClassObj) {
                continuation.resume(response)
            }

            override fun received(response: Code) {
                continuation.resume(null)
            }

            override fun received(response: Context) {
                continuation.resume(null)
            }

            override fun received(response: ErrorObj) {
                continuation.resume(null)
            }

            override fun received(response: Field) {
                continuation.resume(null)
            }

            override fun received(response: Func) {
                continuation.resume(null)
            }

            override fun received(response: Instance) {
                continuation.resume(null)
            }

            override fun received(response: Library) {
                continuation.resume(null)
            }

            override fun received(response: Null) {
                continuation.resume(null)
            }

            override fun received(response: Obj) {
                continuation.resume(null)
            }

            override fun received(response: Script) {
                continuation.resume(null)
            }

            override fun received(response: Sentinel) {
                continuation.resume(null)
            }

            override fun received(response: TypeArguments) {
                continuation.resume(null)
            }

            override fun onError(error: RPCError) {
                continuation.resume(null)
            }

        })
    }
}


suspend fun VmService.getInstance(isolateId: String, instanceId: String): Instance? {
    return suspendCancellableCoroutine { continuation ->
        getInstance(isolateId,instanceId, object : GetInstanceConsumer{
            override fun received(response: Instance) {
                continuation.resume(response)
            }

            override fun onError(error: RPCError) {
                continuation.resume(null)
            }
        })
    }
}

suspend fun VmService.getObjectWithField(isolateId: String,instanceId: String): Field? {
    return suspendCancellableCoroutine { continuation ->
        getObject(isolateId,instanceId,object : GetObjectConsumer{
            override fun received(response: Breakpoint) {
                continuation.resume(null)
            }

            override fun received(response: ClassObj) {
                continuation.resume(null)
            }

            override fun received(response: Code) {
                continuation.resume(null)
            }

            override fun received(response: Context) {
                continuation.resume(null)
            }

            override fun received(response: ErrorObj) {
                continuation.resume(null)
            }

            override fun received(response: Field) {
                continuation.resume(response)
            }

            override fun received(response: Func) {
                continuation.resume(null)
            }

            override fun received(response: Instance) {
                continuation.resume(null)
            }

            override fun received(response: Library) {
                continuation.resume(null)
            }

            override fun received(response: Null) {
                continuation.resume(null)
            }

            override fun received(response: Obj) {
                continuation.resume(null)
            }

            override fun received(response: Script) {
                continuation.resume(null)
            }

            override fun received(response: Sentinel) {
                continuation.resume(null)
            }

            override fun received(response: TypeArguments) {
                continuation.resume(null)
            }

            override fun onError(error: RPCError) {
                continuation.resume(null)
            }

        })
    }
}


suspend fun VmService.getObjectWithLibrary(isolateId: String,instanceId: String): Library? {
    return suspendCancellableCoroutine { continuation ->
        getObject(isolateId,instanceId,object : GetObjectConsumer{
            override fun received(response: Breakpoint) {
                continuation.resume(null)
            }

            override fun received(response: ClassObj) {
                continuation.resume(null)
            }

            override fun received(response: Code) {
                continuation.resume(null)
            }

            override fun received(response: Context) {
                continuation.resume(null)
            }

            override fun received(response: ErrorObj) {
                continuation.resume(null)
            }

            override fun received(response: Field) {
                continuation.resume(null)
            }

            override fun received(response: Func) {
                continuation.resume(null)
            }

            override fun received(response: Instance) {
                continuation.resume(null)
            }

            override fun received(response: Library) {
                continuation.resume(response)
            }

            override fun received(response: Null) {
                continuation.resume(null)
            }

            override fun received(response: Obj) {
                continuation.resume(null)
            }

            override fun received(response: Script) {
                continuation.resume(null)
            }

            override fun received(response: Sentinel) {
                continuation.resume(null)
            }

            override fun received(response: TypeArguments) {
                continuation.resume(null)
            }

            override fun onError(error: RPCError) {
                continuation.resume(null)
            }

        })
    }
}