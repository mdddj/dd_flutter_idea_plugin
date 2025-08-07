package vm

import com.google.gson.JsonObject
import vm.consumer.ServiceExtensionConsumer
import vm.consumer.VMConsumer
import vm.consumer.VersionConsumer
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

suspend fun VmService.mainIsolates(): IsolateRef? {
    val vm = getVm()
    return vm.getIsolates().find { it.getName() == "main" }
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
                        continuation.resume(gson.fromJson(result, WidgetTreeResponse::class.java))
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
                val resultNode = result.getAsJsonObject("result")
                val str = resultNode?.get("enabled")?.asString ?: return cont.resume(false)
                cont.resume(str == "true")
            }

            override fun onError(error: RPCError) {
                cont.resumeWithException(error.exception)
            }
        }
    )
}