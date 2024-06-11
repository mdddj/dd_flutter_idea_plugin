package shop.itbug.fluttercheckversionx.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import shop.itbug.fluttercheckversionx.icons.MyIcons

class FlutterXRunConfigType :
    ConfigurationTypeBase(ID, "Flutter Application (FlutterX Run)", "Run flutter application", MyIcons.flutter) {

    init {
        addFactory(FlutterXRunConfigurationFactory(this))
    }

    companion object {
        const val ID: String = "FlutterXRunConfiguration"
    }
}