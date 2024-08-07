package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.save.DartFileSaveSettingState
import shop.itbug.fluttercheckversionx.save.dartFileSaveSettingPanel
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import javax.swing.JComponent

class AppConfig(project: Project) : Configurable, Disposable, SearchableConfigurable {


    init {
        println("project config init...:${project.basePath}")
    }

    var model = PluginStateService.getInstance().state ?: AppStateModel()

    private var dioSetting = DioListingUiConfig.getInstance().state ?: DoxListeningSetting()
    private val generaAssetsSettingPanel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
    private val dartSaveSettingState = DartFileSaveSettingState.getInstance().state
    private var generaAssetsSettingPanelModelIs = false
    private var dartFileSaveSettingPanelModelIs = false
    private var generateSettingPanel =
        GeneraAssetsSettingPanel(settingModel = generaAssetsSettingPanel, parentDisposable = this@AppConfig) {
            generaAssetsSettingPanelModelIs = it
        }

    private var dog = dartFileSaveSettingPanel(this, dartSaveSettingState) {
        dartFileSaveSettingPanelModelIs = it
    }

    override fun createComponent(): JComponent {
        return JBTabbedPane().apply {
            add(PluginBundle.get("basic"), panel)
            add(PluginBundle.get("assets.gen"), generateSettingPanel)
//            add("保存后执行", dog)
        }
    }

    val dialog: DialogPanel = settingPanel(model, dioSetting, this) {
        model = it
    }

    private val panel: JComponent get() = dialog

    override fun isModified(): Boolean {
        return dialog.isModified() || generaAssetsSettingPanelModelIs || dartFileSaveSettingPanelModelIs
    }

    override fun apply() {
        dialog.apply()
        generateSettingPanel.doApply()
        dog.apply()
        PluginStateService.getInstance().loadState(model)
        DioListingUiConfig.getInstance().loadState(dioSetting)
        GenerateAssetsClassConfig.getInstance().loadState(generaAssetsSettingPanel)
        DartFileSaveSettingState.getInstance().loadState(dartSaveSettingState)
    }

    override fun getDisplayName(): String {
        return PluginBundle.get("setting.flutterx")
    }

    override fun getId(): String {
        return "flutterx"
    }

    override fun reset() {
        dialog.reset()
        dog.reset()
        super<Configurable>.reset()
    }

    override fun dispose() {

    }


    override fun disposeUIResources() {
        Disposer.dispose(this)
        super<Configurable>.disposeUIResources()
    }
}
