package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.*

@State(name = "FlutterXGlobalConfig", storages = [Storage("FlutterXGlobalConfig.xml")])
@Service(Service.Level.APP)
class FlutterXGlobalConfigService : SimplePersistentStateComponent<FlutterXGlobalConfigService.MyState>(MyState()) {

    class MyState : BaseState() {
        var typeInlayOnLeft by property(false)
    }



    companion object {
        fun getInstance() = service<FlutterXGlobalConfigService>()
    }
}