/*
 * Copyright (c) 2015, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package vm

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInfo
import vm.consumer.*
import vm.element.*
import vm.internal.RequestSink
import vm.internal.VmServiceConst
import vm.internal.WebSocketRequestSink
import vm.logging.Logging
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 内部 {@link VmService} 基类，包含非生成的代码。
 */
abstract class VmServiceBase : UserDataHolderBase(), VmServiceConst {
    /**
     * 在 {@link String} ID 和请求时传递的关联 {@link Consumer} 之间的映射。
     * 在访问此字段之前，请与 {@link #consumerMapLock} 同步。
     */
    private val consumerMap = HashMap<String, Consumer>()

    /**
     * 用于同步访问 {@link #consumerMap} 的对象。
     */
    private val consumerMapLock = Any()

    /**
     * 下一个请求的唯一 ID。
     */
    private val nextId = AtomicInteger()

    /**
     * 转发来自 VM 的 {@link Event} 的对象列表。
     */
    private val vmListeners = ArrayList<VmServiceListener>()

    /**
     * 转发来自 VM 的 {@link Event} 的对象列表。
     */
    private val remoteServiceRunners = HashMap<String, RemoteServiceRunner>()

    /**
     * 进行 Observatory 请求的通道。
     */
    var requestSink: RequestSink? = null

    var runtimeVersion: Version? = null

    var client: HttpClient? = null
    var myWebSocketSession: DefaultClientWebSocketSession? = null
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var listenDataJob: Job? = null

    companion object {
        private val ignoreCallback = object : RemoteServiceCompleter {
            override fun result(result: JsonObject) {
                // ignore
            }

            override fun error(code: Int, message: String, data: JsonObject?) {
                // ignore
            }
        }

        /**
         * 通过指定的 URI 连接到 VM Observatory 服务
         *
         * @return 用于与 VM 服务交互的 API 对象（不为 {@code null}）。
         */
        @Throws(IOException::class)
        fun connect(url: String, listener: VmServiceListener? = null): VmService {
            val vmService = VmService()
            if (listener != null) {
                vmService.addVmServiceListener(listener)
            }
            val connectionLatch = CountDownLatch(1)
            var connectionError: Exception? = null

            try {
                vmService.client = HttpClient(CIO) {
                    install(WebSockets) {
                        pingIntervalMillis = 20_000
                    }
                }

                vmService.coroutineScope.launch {
                    try {
                        vmService.client?.webSocket(url, {}) {
                            vmService.myWebSocketSession = this
                            vmService.requestSink = WebSocketRequestSink(this, this)
                            vmService.connectionOpened()
                            Logging.getLogger().logInformation("Dart VM 连接成功。: $url")
                            connectionLatch.countDown()
                            // 监听传入的消息
                            vmService.listenData()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        connectionError = e
                        connectionLatch.countDown()
                        Logging.getLogger().logError("WebSocket error: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                throw IOException("Failed to create websocket: $url", e)
            }

            // 等待连接建立
            if (!connectionLatch.await(3, TimeUnit.SECONDS)) {
                throw IOException("Connection timeout")
            }

            connectionError?.let { throw IOException("Connection failed", it) }



            return vmService
        }

        /**
         * 连接到给定本地端口上的 VM Observatory 服务。
         *
         * @return 用于与 VM 服务交互的 API 对象（不为 {@code null}）。
         * @deprecated 更推荐使用基于 URL 的构造函数 {@link VmServiceBase#connect}
         */
        @Deprecated("")
        @Throws(IOException::class)
        fun localConnect(port: Int): VmService {
            return connect("ws://localhost:$port/ws")
        }

        val APP_ID_KEY = Key.create<String>("vm service appid")
        val APP_INFO = Key.create<FlutterAppInfo>("DartProjectService.APP_INFO")
    }


    /**
     * 添加一个监听器以接收来自 VM 的 {@link Event}。
     */
    fun addVmServiceListener(listener: VmServiceListener) {
        vmListeners.add(listener)
    }

    fun close() {
        connectionClosed()
        listenDataJob?.cancel()
        listenDataJob = null
        coroutineScope.cancel()
        myWebSocketSession?.cancel()
        myWebSocketSession = null
    }

    /**
     * 从 VM 中移除给定的监听器。
     */
    fun removeVmServiceListener(listener: VmServiceListener) {
        vmListeners.remove(listener)
    }

    /**
     * 添加一个 VM RemoteServiceRunner。
     */
    fun addServiceRunner(service: String, runner: RemoteServiceRunner) {
        remoteServiceRunners[service] = runner
    }

    /**
     * 移除一个 VM RemoteServiceRunner。
     */
    fun removeServiceRunner(service: String) {
        remoteServiceRunners.remove(service)
    }


    /**
     * 断开与 VM Observatory 服务的连接。
     */
    fun disconnect() {

        client?.close()
        requestSink?.close()
        coroutineScope.cancel()

    }

    /**
     * 返回具有给定标识符的实例。
     */
    fun getInstance(isolateId: String, instanceId: String, consumer: GetInstanceConsumer) {
        getObject(isolateId, instanceId, object : GetObjectConsumer {
            override fun onError(error: RPCError) {
                consumer.onError(error)
            }

            override fun received(response: Breakpoint) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: ClassObj) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Code) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Context) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: ErrorObj) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Field) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Func) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Instance) {
                consumer.received(response)
            }

            override fun received(response: Library) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Null) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Obj) {
                if (response is Instance) {
                    consumer.received(response)
                } else {
                    onError(RPCError.unexpected("Instance", response))
                }
            }

            override fun received(response: Script) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: Sentinel) {
                onError(RPCError.unexpected("Instance", response))
            }

            override fun received(response: TypeArguments) {
                onError(RPCError.unexpected("Instance", response))
            }
        })
    }

    /**
     * 返回具有给定标识符的库。
     */
    fun getLibrary(isolateId: String, libraryId: String, consumer: GetLibraryConsumer) {
        getObject(isolateId, libraryId, object : GetObjectConsumer {
            override fun onError(error: RPCError) {
                consumer.onError(error)
            }

            override fun received(response: Breakpoint) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: ClassObj) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Code) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Context) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: ErrorObj) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Field) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Func) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Instance) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Library) {
                consumer.received(response)
            }

            override fun received(response: Null) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Obj) {
                if (response is Library) {
                    consumer.received(response)
                } else {
                    onError(RPCError.unexpected("Library", response))
                }
            }

            override fun received(response: Script) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: Sentinel) {
                onError(RPCError.unexpected("Library", response))
            }

            override fun received(response: TypeArguments) {
                onError(RPCError.unexpected("Library", response))
            }
        })
    }

    abstract fun getObject(isolateId: String, objectId: String, consumer: GetObjectConsumer)

    /**
     * 调用特定的服务协议扩展方法。
     * <p>
     * 参见 https://api.dart.dev/stable/dart-developer/dart-developer-library.html。
     */
    fun callServiceExtension(isolateId: String, method: String, consumer: ServiceExtensionConsumer) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request(method, params, consumer)
    }

    fun callServiceExtension(isolateId: String, method: String) {
        val params = JsonObject()
        params.addProperty("isolateId", isolateId)
        request(method, params, defaultServiceExtensionConsumer({}) {  })
    }
    fun callServiceExtension(
        isolateId: String,
        method: String,
        params: JsonObject,
    ) {
        params.addProperty("isolateId", isolateId)
        request(method, params, defaultServiceExtensionConsumer({}) {  })
    }
    /**
     * 调用特定的服务协议扩展方法。
     * <p>
     * 参见 https://api.dart.dev/stable/dart-developer/dart-developer-library.html。
     */
    fun callServiceExtension(
        isolateId: String,
        method: String,
        params: JsonObject,
        consumer: ServiceExtensionConsumer
    ) {
        params.addProperty("isolateId", isolateId)
        request(method, params, consumer)
    }

    /**
     * 发送请求并将请求与传递的 {@link Consumer} 关联。
     */
    protected fun request(method: String, params: JsonObject, consumer: Consumer) {
        // Assemble the request
        val id = nextId.incrementAndGet().toString()
        val request = JsonObject()
        request.addProperty(VmServiceConst.JSONRPC, VmServiceConst.JSONRPC_VERSION)
        request.addProperty(VmServiceConst.ID, id)
        request.addProperty(VmServiceConst.METHOD, method)
        request.add(VmServiceConst.PARAMS, params)

        // Cache the consumer to receive the response
        synchronized(consumerMapLock) {
            consumerMap[id] = consumer
        }
        // Send the request
        requestSink?.add(request)
    }

    fun connectionOpened() {
        for (listener in ArrayList(vmListeners)) {
            try {
                listener.connectionOpened()
            } catch (e: Exception) {
                Logging.getLogger().logError("Exception notifying listener", e)
            }
        }
    }

    private fun forwardEvent(streamId: String, event: Event) {
        for (listener in ArrayList(vmListeners)) {
            try {
                listener.received(streamId, event)
            } catch (e: Exception) {
                Logging.getLogger().logError("Exception processing event: $streamId, ${event.json}", e)
            }
        }
    }

    fun connectionClosed() {
        for (listener in ArrayList(vmListeners)) {
            try {
                listener.connectionClosed()
            } catch (e: Exception) {
                Logging.getLogger().logError("Exception notifying listener", e)
            }
        }
    }

    protected abstract fun forwardResponse(consumer: Consumer, type: String, json: JsonObject)

    protected fun logUnknownResponse(consumer: Consumer, json: JsonObject) {
        val consumerClass = consumer.javaClass
        val msg = StringBuilder()
        msg.append("Expected response for $consumerClass\n")
        for (interf in consumerClass.interfaces) {
            msg.append("  implementing $interf\n")
        }
        msg.append("  but received $json")
        Logging.getLogger().logError(msg.toString())
    }

    /**
     * 处理来自 VM 服务的响应，并将该响应转发给与响应 ID 关联的消费者。
     */
    open fun processMessage(jsonText: String?) {
        if (jsonText == null || jsonText.isEmpty()) {
            return
        }

        // Decode the JSON
        val json: JsonObject
        try {
            json = JsonParser.parseString(jsonText).asJsonObject
        } catch (e: Exception) {
            Logging.getLogger().logError("Parse message failed: $jsonText", e)
            return
        }

        when {
            json.has("method") -> {
                if (!json.has(VmServiceConst.PARAMS)) {
                    val message = "Missing ${VmServiceConst.PARAMS}"
                    Logging.getLogger().logError(message)
                    val response = JsonObject()
                    response.addProperty(VmServiceConst.JSONRPC, VmServiceConst.JSONRPC_VERSION)
                    val error = JsonObject()
                    error.addProperty(VmServiceConst.CODE, VmServiceConst.INVALID_REQUEST)
                    error.addProperty(VmServiceConst.MESSAGE, message)
                    response.add(VmServiceConst.ERROR, error)
                    requestSink?.add(response)
                    return
                }
                if (json.has("id")) {
                    processRequest(json)
                } else {
                    processNotification(json)
                }
            }

            json.has("result") || json.has("error") -> {
                processResponse(json)
            }

            else -> {
                Logging.getLogger().logError("Malformed message")
            }
        }
    }

    private fun processRequest(json: JsonObject) {
        val response = JsonObject()
        response.addProperty(VmServiceConst.JSONRPC, VmServiceConst.JSONRPC_VERSION)

        // Get the consumer associated with this request
        val id: String
        try {
            id = json[VmServiceConst.ID].asString
        } catch (e: Exception) {
            val message = "Request malformed ${VmServiceConst.ID}"
            Logging.getLogger().logError(message, e)
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, VmServiceConst.INVALID_REQUEST)
            error.addProperty(VmServiceConst.MESSAGE, message)
            response.add(VmServiceConst.ERROR, error)
            requestSink?.add(response)
            return
        }

        response.addProperty(VmServiceConst.ID, id)

        val method: String
        try {
            method = json[VmServiceConst.METHOD].asString
        } catch (e: Exception) {
            val message = "Request malformed ${VmServiceConst.METHOD}"
            Logging.getLogger().logError(message, e)
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, VmServiceConst.INVALID_REQUEST)
            error.addProperty(VmServiceConst.MESSAGE, message)
            response.add(VmServiceConst.ERROR, error)
            requestSink?.add(response)
            return
        }

        val params: JsonObject
        try {
            params = json[VmServiceConst.PARAMS].asJsonObject
        } catch (e: Exception) {
            val message = "Request malformed ${VmServiceConst.METHOD}"
            Logging.getLogger().logError(message, e)
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, VmServiceConst.INVALID_REQUEST)
            error.addProperty(VmServiceConst.MESSAGE, message)
            response.add(VmServiceConst.ERROR, error)
            requestSink?.add(response)
            return
        }

        if (!remoteServiceRunners.containsKey(method)) {
            val message = "Unknown service $method"
            Logging.getLogger().logError(message)
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, VmServiceConst.METHOD_NOT_FOUND)
            error.addProperty(VmServiceConst.MESSAGE, message)
            response.add(VmServiceConst.ERROR, error)
            requestSink?.add(response)
            return
        }

        val runner = remoteServiceRunners[method]
        try {
            runner?.run(params, object : RemoteServiceCompleter {
                override fun result(result: JsonObject) {
                    response.add(VmServiceConst.RESULT, result)
                    requestSink?.add(response)
                }

                override fun error(code: Int, message: String, data: JsonObject?) {
                    val error = JsonObject()
                    error.addProperty(VmServiceConst.CODE, code)
                    error.addProperty(VmServiceConst.MESSAGE, message)
                    if (data != null) {
                        error.add(VmServiceConst.DATA, data)
                    }
                    response.add(VmServiceConst.ERROR, error)
                    requestSink?.add(response)
                }
            })
        } catch (e: Exception) {
            val message = "Internal Server Error"
            Logging.getLogger().logError(message, e)
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, VmServiceConst.SERVER_ERROR)
            error.addProperty(VmServiceConst.MESSAGE, message)
            response.add(VmServiceConst.ERROR, error)
            requestSink?.add(response)
        }
    }

    private fun processNotification(json: JsonObject) {
        val method: String
        try {
            method = json[VmServiceConst.METHOD].asString
        } catch (e: Exception) {
            Logging.getLogger().logError("Request malformed ${VmServiceConst.METHOD}", e)
            return
        }
        val params: JsonObject
        try {
            params = json[VmServiceConst.PARAMS].asJsonObject
        } catch (e: Exception) {
            Logging.getLogger().logError("Event missing ${VmServiceConst.PARAMS}", e)
            return
        }
        if ("streamNotify" == method) {
            val streamId: String
            try {
                streamId = params[VmServiceConst.STREAM_ID].asString
            } catch (e: Exception) {
                Logging.getLogger().logError("Event missing ${VmServiceConst.STREAM_ID}", e)
                return
            }
            val event: Event
            try {
                event = Event(params[VmServiceConst.EVENT].asJsonObject)
            } catch (e: Exception) {
                Logging.getLogger().logError("Event missing ${VmServiceConst.EVENT}", e)
                return
            }
            forwardEvent(streamId, event)
        } else {
            if (!remoteServiceRunners.containsKey(method)) {
                Logging.getLogger().logError("Unknown service $method")
                return
            }

            val runner = remoteServiceRunners[method]
            try {
                runner?.run(params, ignoreCallback)
            } catch (e: Exception) {
                Logging.getLogger().logError("Internal Server Error", e)
            }
        }
    }

    protected fun removeNewLines(str: String): String {
        return str.replace("\r\n", " ").replace("\n", " ")
    }

    private fun processResponse(json: JsonObject) {
        val idElem = json[VmServiceConst.ID]
        if (idElem == null) {
            Logging.getLogger().logError("Response missing ${VmServiceConst.ID}")
            return
        }

        // Get the consumer associated with this response
        val id: String
        try {
            id = idElem.asString
        } catch (e: Exception) {
            Logging.getLogger().logError("Response missing ${VmServiceConst.ID}", e)
            return
        }
        val consumer: Consumer?
        synchronized(consumerMapLock) {
            consumer = consumerMap.remove(id)
        }

        if (consumer == null) {
            Logging.getLogger().logError("No consumer associated with ${VmServiceConst.ID}: $id")
            return
        }

        // Forward the response if the request was successfully executed
        var resultElem = json[VmServiceConst.RESULT]
        if (resultElem != null) {
            val result: JsonObject
            try {
                result = resultElem.asJsonObject
            } catch (e: Exception) {
                Logging.getLogger().logError("Response has invalid ${VmServiceConst.RESULT}", e)
                return
            }
            var responseType = ""
            if (result.has(VmServiceConst.TYPE)) {
                responseType = result[VmServiceConst.TYPE].asString
            }
            // ServiceExtensionConsumers do not care about the response type.
            else if (consumer !is ServiceExtensionConsumer) {
                Logging.getLogger().logError("Response missing ${VmServiceConst.TYPE}: $result")
                return
            }
            forwardResponse(consumer, responseType, result)
            return
        }

        // Forward an error if the request failed
        resultElem = json[VmServiceConst.ERROR]
        if (resultElem != null) {
            val error: JsonObject
            try {
                error = resultElem.asJsonObject
            } catch (e: Exception) {
                Logging.getLogger().logError("Response has invalid ${VmServiceConst.RESULT}", e)
                return
            }
            consumer.onError(RPCError(error))
            return
        }

        Logging.getLogger().logError("Response missing ${VmServiceConst.RESULT} and ${VmServiceConst.ERROR}")
    }
}
