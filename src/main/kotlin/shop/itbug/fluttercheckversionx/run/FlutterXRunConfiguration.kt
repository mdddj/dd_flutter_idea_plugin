package shop.itbug.fluttercheckversionx.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class FlutterXRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
    RunConfigurationBase<FlutterXRunConfigOptions>(project, configurationFactory, name) {

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler {
                val command = GeneralCommandLine(options.script.split(" "))
                command.setWorkDirectory(project.basePath)
                val createProcessHandler = ProcessHandlerFactory.getInstance().createProcessHandler(command)
                ProcessTerminatedListener.attach(createProcessHandler)
                return createProcessHandler
            }
        }
    }


    override fun getOptions(): FlutterXRunConfigOptions {
        return super.getOptions() as FlutterXRunConfigOptions
    }

    fun getMyOptions(): FlutterXRunConfigOptions = options

    fun setNewOptions(newOpt: FlutterXRunConfigOptions) {
        loadState(newOpt)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return FlutterXRunOptionEditor(this)
    }


}