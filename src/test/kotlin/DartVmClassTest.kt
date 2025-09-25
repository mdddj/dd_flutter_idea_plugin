
import com.google.gson.GsonBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import vm.VmService
import vm.VmServiceBase
import vm.VmServiceListener
import vm.devtool.*
import vm.element.Event
import vm.logging.Logger
import vm.logging.Logging

class DartVmClassTest : BasePlatformTestCase() {
    val json = GsonBuilder().setPrettyPrinting().create()

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

    val url = "ws://127.0.0.1:63210/vJxUYNzr9gM=/ws"
    val createVmService: VmService
        get() = VmServiceBase.connect(url, vmListen)


    //获取 provider 提供者
    fun testGetProviders() {
        runBlocking {
            val vmService = createVmService
            Logging.setLogger(Logger.CONSOLE)
            vmService.updateMainIsolateId()
            val providers: List<ProviderNode> = ProviderHelper.getProviderNodes(vmService)
            assert(providers.isNotEmpty())
            for (provider in providers) {
                //获取对象的详情
                val path = provider.getProviderPath()
                val resultByPath = ProviderHelper.getInstanceDetails(vmService, path)
                println(resultByPath)
                when (resultByPath) {
                    is InstanceDetails.Object -> {
                        resultByPath.fieldsFiltered.forEach { field ->
                            val newPath: InstancePath.FromInstanceId = path.pathForChildWithInstance(
                                PathToProperty.ObjectProperty(field.name, field.ownerUri, field.ownerName, field),
                                instanceId = field.ref!!.getId()!!
                            )
                            val instance: InstanceDetails =  ProviderHelper.getInstanceDetails(vmService,newPath,resultByPath)
                            println(instance)
                        }
                    }

                    else -> {

                    }
                }
            }
        }
    }
}
