import com.google.gson.GsonBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import vm.VmService
import vm.VmServiceBase
import vm.VmServiceListener
import vm.devtool.*
import vm.element.Event
import vm.getInstance
import vm.logging.Logger
import vm.logging.Logging

class DartVmClassTest : BasePlatformTestCase() {
    val json = GsonBuilder().setPrettyPrinting().create()

    override fun getTestDataPath(): String {
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

    val url = "ws://127.0.0.1:65197/SBrTL9VPO4c=/ws"
    val createVmService: VmService
        get() = VmServiceBase.connect(url, vmListen)


    //获取 provider 提供者
    fun testGetProviders() {
        runBlocking {
            val vmService = createVmService
            Logging.setLogger(Logger.IDEA)
            vmService.updateMainIsolateId()
            val providers: List<ProviderNode> = ProviderHelper.getProviderNodes(vmService)
            val mainIsolateId = vmService.getMainIsolateId()
            assert(providers.isNotEmpty())
            for (provider in providers) {
                val path = provider.getProviderPath()
                when (val resultByPath = ProviderHelper.getInstanceDetails(vmService, path)) {
                    is InstanceDetails.Object -> {
                        resultByPath.fields.forEach { field ->
                            val newPath: InstancePath.FromInstanceId = path.pathForChildWithInstance(
                                PathToProperty.ObjectProperty(field.name, field.ownerUri, field.ownerName, field),
                                instanceId = field.ref.getId()
                            )

                            val instance = vmService.getInstance(mainIsolateId, field.ref.getId())
                            println("instance:$instance")
                        }
                    }

                    else -> {

                    }
                }
            }
        }
    }
}
