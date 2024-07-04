package shop.itbug.fluttercheckversionx.run

import com.intellij.execution.configurations.RunConfigurationOptions


data class FlutterXRunConfigOptions(var script: String = "flutter run") : RunConfigurationOptions()