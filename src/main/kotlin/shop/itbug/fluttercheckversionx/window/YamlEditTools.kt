package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarProvider
import org.apache.commons.logging.LogFactory
import org.jetbrains.yaml.YAMLLanguage


/**
 * 在yaml编辑器中添加一个搜索的图标
 */
class YamlEditTools : FloatingToolbarProvider {
    private val log = LogFactory.getLog(YamlEditTools::class.java)
    override val actionGroup: ActionGroup
        get() = ActionManager.getInstance().getAction("Yaml.Search") as DefaultActionGroup

    override val autoHideable: Boolean
        get() = false

    override fun register(dataContext: DataContext, component: FloatingToolbarComponent, parentDisposable: Disposable) {
       val language = dataContext.getData(CommonDataKeys.LANGUAGE)
        log.info("注册yaml搜索包的浮动按钮:${language?.displayName}")
        if(language == YAMLLanguage.INSTANCE){
            log.info("注册yaml编辑器搜索按钮成功")
            super.register(dataContext, component, parentDisposable)
            component.scheduleShow()
        }

    }
}