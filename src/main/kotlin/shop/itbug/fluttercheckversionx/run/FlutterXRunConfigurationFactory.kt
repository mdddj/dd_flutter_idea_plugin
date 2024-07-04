package shop.itbug.fluttercheckversionx.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class FlutterXRunConfigurationFactory(configurationType: ConfigurationType) :
    ConfigurationFactory(configurationType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return FlutterXRunConfiguration(project, this, "FlutterX Run")
    }


    override fun getId(): String {
        return FlutterXRunConfigType.ID
    }


    override fun getOptionsClass(): Class<out BaseState> {
        return FlutterXRunConfigOptions::class.java
    }

}