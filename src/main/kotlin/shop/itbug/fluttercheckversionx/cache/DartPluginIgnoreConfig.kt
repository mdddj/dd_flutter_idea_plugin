package shop.itbug.fluttercheckversionx.cache

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "DartPluginIgnoreConfig", storages = [Storage("DartPluginIgnoreConfig.xml")])
class DartPluginIgnoreConfig private constructor() : PersistentStateComponent<DartPluginIgnoreConfig>{


     var names = mutableListOf<String>() //忽略的插件名字

    override fun getState(): DartPluginIgnoreConfig {
        return this
    }

    override fun loadState(state: DartPluginIgnoreConfig) {
        XmlSerializerUtil.copyBean(state, this)
    }

    ///忽略检测插件名
    fun add(pluginName: String) {
        if(names.contains(pluginName).not()){
            names.add(pluginName)
            loadState(this)
        }else{
            remove(pluginName)
        }
    }


    ///移除忽略检测插件名
    fun remove(pluginName: String) {
        if(names.contains(pluginName)){
            names.remove(pluginName)
            loadState(this)
        }

    }


    fun isIg(pluginName: String): Boolean {
        return names.contains(pluginName)
    }

    companion object {

        ///获取实例
        fun getInstance(project: Project) : DartPluginIgnoreConfig {
            return project.getService(DartPluginIgnoreConfig::class.java).state
        }


    }


}