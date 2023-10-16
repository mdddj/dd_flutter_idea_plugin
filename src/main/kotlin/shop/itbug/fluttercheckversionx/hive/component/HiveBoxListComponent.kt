package shop.itbug.fluttercheckversionx.hive.component

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.hive.model.HiveActionGetKeys
import shop.itbug.fluttercheckversionx.hive.model.HiveActionGetValue
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

///盒子列表
class HiveBoxListComponent(project: Project) : OnePixelSplitter() {


    private val hiveBoxList = HiveBoxList()
    private val keysList = HiveKeysList(hiveBoxList)

    init {
        firstComponent = hiveBoxList.scroll().apply {
            border = null
        }
        secondComponent = keysList.scroll().apply {
            border = null
        }
        splitterProportionKey = "hive-box-and-list"
    }
}

///盒子列表
class HiveBoxList : JBList<String>(), DioApiService.NativeMessageProcessing, ListSelectionListener {

    init {
        DioApiService.addHandle(this)
        cellRenderer = ItemRender()
        addListSelectionListener(this)
    }


    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?) {
        jsonObject?.apply {
            val type = getString("type")
            if (type == "getBoxList") {
                val arr = getJSONArray("data").map { it.toString() }
                model = ItemModel(arr)
            }
        }
    }


    private inner class ItemModel(list: List<String>) : DefaultListModel<String>() {
        init {
            addAll(list)
        }
    }

    private inner class ItemRender : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            append(value ?: "")
        }

    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            val projectName = service<AppService>().currentSelectName.get() ?: ""
            DioApiService.sendByAnyObject(HiveActionGetKeys(projectName = projectName, boxName = selectedValue))
        }
    }

}


///盒子里面的 key 列表
class HiveKeysList(private val boxList: JBList<String>) : JBList<String>(), DioApiService.NativeMessageProcessing,
    ListSelectionListener {


    init {
        cellRenderer = ItemRender()
        addListSelectionListener(this)
        register()
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?) {
        jsonObject?.apply {
            val type = getString("type")
            if (type == "getKeys") {
                val arr = getJSONArray("data").map { it.toString() }
                model = ItemModel(arr)
            }
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            val projectName = service<AppService>().currentSelectName.get() ?: ""
            val boxName = boxList.selectedValue
            DioApiService.sendByAnyObject(
                HiveActionGetValue(
                    projectName = projectName, boxName = boxName, key = selectedValue
                )
            )
        }

    }


    private inner class ItemModel(list: List<String>) : DefaultListModel<String>() {
        init {
            addAll(list)
        }
    }

    private inner class ItemRender : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            append(value ?: "")
        }

    }

    fun clean() {
        model = ItemModel(emptyList())
    }

}