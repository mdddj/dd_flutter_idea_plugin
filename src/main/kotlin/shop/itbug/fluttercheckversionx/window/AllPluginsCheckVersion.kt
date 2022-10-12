package shop.itbug.fluttercheckversionx.window

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.model.FlutterPluginElementModel
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import shop.itbug.fluttercheckversionx.model.getElementVersion
import shop.itbug.fluttercheckversionx.model.isLastVersion
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

///检测版本小窗口
class AllPluginsCheckVersion(val project: Project) : JPanel(BorderLayout()) {

    private var topTipLabel = JBLabel("检测插件版本")
    private var bottomTipLabel = JBLabel("等待检测中")

    //插件列表
    private var plugins: MutableMap<FlutterPluginType, List<FlutterPluginElementModel>> =
        MyPsiElementUtil.getAllFlutters(project)

    //展示列表组件
    private val listView = JBList<FlutterPluginElementModel>()

    //

    init {
        add(topTipLabel, BorderLayout.NORTH)
        add(JBScrollPane(listView), BorderLayout.CENTER)
        add(bottomTipLabel,BorderLayout.SOUTH)
        listView.model = PluginListModel(emptyList())
        listView.cellRenderer = PluginListCellRender()
        initRequest()
    }

    /**
     * 开始检测插件新版本
     * 需要访问网络
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun initRequest() {
        topTipLabel.text = "检测中..."
        GlobalScope.launch {
            for (item in plugins.values) {
                bottomTipLabel.text = "获取到插件数量:${item.size}"
                item.forEach { model ->
                    bottomTipLabel.text = "正在检测${model.name}"
                    val r = ServiceCreate.create<PubService>().callPluginDetails(model.name).execute()
                    if (r.isSuccessful) {

                        val rModel = r.body()
                        if (rModel != null) {
                            model.pubData = rModel
                            if(!model.isLastVersion()){
                                val oldList = (listView.model as PluginListModel).list
                                val l = oldList.toMutableList()
                                l.add(model)
                                listView.model = PluginListModel(l)
                            }

                        }
                    }
                }
                bottomTipLabel.text = "检测完成"
            }
        }
    }


}


class PluginListModel(val list: List<FlutterPluginElementModel>) : AbstractListModel<FlutterPluginElementModel>() {
    override fun getSize(): Int {
        return list.size
    }

    override fun getElementAt(index: Int): FlutterPluginElementModel {
        return list[index]
    }
}

class PluginListCellRender : ListCellRenderer<FlutterPluginElementModel> {
    override fun getListCellRendererComponent(
        list: JList<out FlutterPluginElementModel>?,
        value: FlutterPluginElementModel?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        val box = Box.createVerticalBox()
        val nameLabel = JBLabel(value!!.name)
        box.add(nameLabel)


        val titleLabel = JBLabel("有新版本:${value.pubData?.latest?.version}(${value.getElementVersion()})")
        box.add(titleLabel)


        return box
    }

}