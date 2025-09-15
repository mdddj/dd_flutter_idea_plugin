import com.google.gson.GsonBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import vm.VmService
import vm.VmServiceBase
import vm.VmServiceListener
import vm.devtool.InstanceDetails
import vm.devtool.PathToProperty
import vm.devtool.ProviderHelper
import vm.devtool.ProviderNode
import vm.element.Event

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

    val url = "ws://127.0.0.1:60944/fuEL9uYgCUA=/ws"
    val createVmService: VmService
        get() = VmServiceBase.connect(url, vmListen)


    //获取 provider 提供者
    fun testGetProviders() {
        runBlocking {
            val vmService = createVmService
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
                            val newPath = path.pathForChildWithInstance(
                                PathToProperty.ObjectProperty(field.name, field.ownerUri, field.ownerName, field),
                                instanceId = field.ref!!.getId()!!
                            )
                            val instance =  ProviderHelper.getInstanceDetails(vmService,newPath,resultByPath)
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
