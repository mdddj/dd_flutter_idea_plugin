import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import vm.*
import vm.consumer.GetMemoryUsageConsumer
import vm.consumer.ProtocolListConsumer
import vm.element.*
import vm.logging.Logger
import vm.logging.Logging

class DartVmTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String? {
        return "src/test/testData"
    }

    val vmListen =
        object : VmServiceListener {
            override fun connectionOpened() {
                println("连接打开")
            }

            override fun received(streamId: String, event: Event) {
                println("连接被拒绝:${streamId}  $event")
            }

            override fun connectionClosed() {
                println("连接断开")
            }
        }

    val url = "ws://127.0.0.1:51279/O7dUpbtsCdM=/ws"
    val createVmService
        get() = VmServiceBase.connect(url, vmListen)

    // 测试 dart vm 连接
    fun testConnect() {
        runBlocking {
            Logging.setLogger(Logger.CONSOLE)
            val vmService = createVmService
            vmService.runtimeVersion = vmService.getVersion()
            val mainIos = vmService.mainIsolates()
            assertNotNull(mainIos)
            assertTrue(mainIos!!.getId() != null)
            Logging.getLogger().logInformation("开始获取 widget信息")
            val widgets = vmService.getRootWidgetTree(mainIos.getId()!!, "testGroup")
            Logging.getLogger().logInformation("结束获取 widgets信息")
            assertNotNull(widgets)

            delay(3000)
            assertTrue(vmService.myWebSocketSession != null)
            assertTrue(vmService.runtimeVersion != null)
            delay(1000)
            vmService.close()
        }
    }

    // 获取可支持协议
    fun testGetSupportProtocolList() {
        Logging.setLogger(Logger.CONSOLE)
        val vmService = createVmService
        runBlocking {
            vmService.getSupportedProtocols(
                object : ProtocolListConsumer {
                    override fun received(response: ProtocolList) {
                        println(response)
                    }

                    override fun onError(error: RPCError) {}
                }
            )
            delay(1000)
        }
    }

    // 获取 vm 内存使用率
    fun testMemoryUsage() {
        Logging.setLogger(Logger.CONSOLE)
        val vmService = createVmService

        runBlocking {
            val id = vmService.mainIsolates()
            vmService.getMemoryUsage(
                id!!.getId()!!,
                object : GetMemoryUsageConsumer {
                    override fun received(response: MemoryUsage) {
                        log(response)
                    }

                    override fun received(response: Sentinel) {
                        log(response)
                    }

                    override fun onError(error: RPCError) {}
                }
            )
            delay(1000)
        }
    }

    // 获取 widget 节点详细信息
    fun testGetProperties() {
        Logging.setLogger(Logger.CONSOLE)
        val vmService = createVmService
        runBlocking {
            val id = vmService.mainIsolates()?.getId()!!
            val tree =
                vmService.getRootWidgetTree(
                    isolateId = id,
                    groupName = "test-group",
                    isSummaryTree = true,
                    withPreviews = true,
                    fullDetails = true
                )
            val nodeId =
                tree?.result
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.valueId
                    ?: ""
            //            val response =
            //                vmService.getDetailsSubtree(
            //                    id,
            //                    "test-group", nodeId
            //                )
            val response = vmService.getProperties(id, "test-group", nodeId)
            log(response)
            delay(1000)
        }
    }

    // 控制 inspector 显示隐藏
    fun testSetInspectorOverlay() {
        val vm = createVmService
        runBlocking {
            val id = vm.mainIsolates()?.getId()
            id?.let {
                vm.setInspectorOverlay(id, true)
                delay(3000)
                vm.setInspectorOverlay(id, false)
            }
        }
    }

    // widget 树点击获取数据
    fun testWidgetTreeClick() {
        Logging.setLogger(Logger.CONSOLE)
        val vmService = createVmService
        runBlocking {
            val id = vmService.mainIsolates()?.getId()!!
            val tree =
                vmService.getRootWidgetTree(
                    isolateId = id,
                    groupName = "test-group",
                    isSummaryTree = true,
                    withPreviews = true,
                    fullDetails = true
                )
            val nodeId =
                tree?.result
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.children
                    ?.firstOrNull()
                    ?.valueId
                    ?: ""

            // 测试获取属性，模拟树节点点击
            val response = vmService.getProperties(id, "test-group", nodeId)
            log("Widget properties response: $response")



            delay(1000)
        }
    }

    fun log(m: Any) {
        Logging.getLogger().logInformation("$m")
    }
}
