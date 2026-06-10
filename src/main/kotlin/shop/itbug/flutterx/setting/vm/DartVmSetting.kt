package shop.itbug.flutterx.setting.vm

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import shop.itbug.flutterx.api.vm.DartVmDevToolExtensionBean
import shop.itbug.flutterx.common.dart.FlutterXVMService
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.window.getDartVmWindow
import shop.itbug.flutterx.window.refreshDartVmWindowContents
import javax.swing.JComponent

class DartVmSetting(val project: Project) : Configurable {
    private lateinit var myPanel: DialogPanel
    private val state = PluginConfig.getState(project)
    private val setting = FlutterXVMService.getInstance(project)
    private val hiddenDartVmDevToolTabIds = mutableSetOf<String>()

    override fun getDisplayName():  String {
        return "Dart VM Settings"
    }

    override fun createComponent(): JComponent {
        hiddenDartVmDevToolTabIds.clear()
        hiddenDartVmDevToolTabIds.addAll(state.hiddenDartVmDevToolTabIds)
        val devToolTabs = getDevToolTabs()
        myPanel = panel {
            row {
                checkBox("Enable Dart VM tool window").bindSelected(state::enableVmServiceToolWindow)
                    .comment("(only this project)")
            }
            row {
                checkBox("Listen to vm service connection").bindSelected(state::enableVmServiceListen)
                    .comment("Disabled, vm service related services are unavailable (only this project) ")
            }
            group("Dart VM Tabs") {
                if (devToolTabs.isEmpty()) {
                    row {
                        comment("No Dart VM tabs are registered.")
                    }
                } else {
                    devToolTabs.forEach { tab ->
                        row {
                            checkBox(tab.title)
                                .bindSelected(
                                    { tab.id !in hiddenDartVmDevToolTabIds },
                                    { selected ->
                                        if (selected) {
                                            hiddenDartVmDevToolTabIds.remove(tab.id)
                                        } else {
                                            hiddenDartVmDevToolTabIds.add(tab.id)
                                        }
                                    }
                                )
                                .apply {
                                    tab.description?.let { comment(it) }
                                }
                        }
                    }
                    row {
                        comment("Unchecked tabs are hidden. New third-party tabs are shown by default.")
                    }
                }
            }
            row("") {
                comment(PluginBundle.get("setting.reset.tip"))
            }
        }
        return myPanel
    }

    override fun isModified(): Boolean {
        return myPanel.isModified()
    }

    override fun apply() {
        myPanel.apply()
        PluginConfig.getState(project).enableVmServiceToolWindow = state.enableVmServiceToolWindow
        PluginConfig.getState(project).enableVmServiceListen = state.enableVmServiceListen
        PluginConfig.getState(project).replaceHiddenDartVmDevToolTabIds(hiddenDartVmDevToolTabIds)
        setting.settingChanged()
        updateDartVmWindowState()

    }

    override fun disposeUIResources() {
        updateDartVmWindowState()
        super.disposeUIResources()
    }

    private fun updateDartVmWindowState() {
        if (!state.enableVmServiceToolWindow) {
            project.getDartVmWindow()?.let {
                it.hide()
                it.isAvailable = false
            }
        }else {
            project.getDartVmWindow()?.let {
                it.isAvailable = true
                project.refreshDartVmWindowContents()
            }
        }
    }

    private fun getDevToolTabs(): List<DartVmDevToolTabOption> {
        return DartVmDevToolExtensionBean.EP_NAME.extensionList.mapNotNull { extension ->
            try {
                val impl = extension.createExtension()
                DartVmDevToolTabOption(extension.id, impl.getTabTitle(project), extension.getDescription())
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Throwable) {
                LOG.warn("Failed to read Dart VM dev tool tab metadata from ${extension.implementation}", e)
                null
            }
        }
    }

    private data class DartVmDevToolTabOption(
        val id: String,
        val title: String,
        val description: String?
    )

    companion object {
        private val LOG = Logger.getInstance(DartVmSetting::class.java)
    }
}
