package shop.itbug.flutterx.hive.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.flutterx.common.scroll
import shop.itbug.flutterx.hive.model.HiveActionGetKeys
import shop.itbug.flutterx.hive.model.HiveActionGetValue
import shop.itbug.flutterx.socket.service.AppService
import shop.itbug.flutterx.socket.service.DioApiService
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

///盒子列表
class HiveBoxListComponent : OnePixelSplitter(), Disposable {


    private val hiveBoxList = HiveBoxList()
    private val keysList = HiveKeysList(hiveBoxList)

    init {
        firstComponent = BorderLayoutPanel().apply {
            addToTop(JBLabel("Box list").apply {
                border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
                font = JBFont.label()
                foreground = UIUtil.getLabelDisabledForeground()
            })
            addToCenter(hiveBoxList.scroll().apply {
                border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
            })
            border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        }
        secondComponent = BorderLayoutPanel().apply {
            addToTop(JBLabel("Key list").apply {
                border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
                font = JBFont.label()
                foreground = UIUtil.getLabelDisabledForeground()
            })
            addToCenter(keysList.scroll().apply {
                border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
            })
            border = JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0)
        }
        splitterProportionKey = "hive-box-and-list"
        Disposer.register(this, keysList)
        Disposer.register(this, hiveBoxList)
    }

    override fun dispose() {
        println("hive box list component disposed")
        super.dispose()
    }
}

///盒子列表
class HiveBoxList : JBList<String>(), DioApiService.NativeMessageProcessing, ListSelectionListener, Disposable {
    private val gson = DioApiService.getInstance().gson

    init {
        DioApiService.getInstance().addHandle(this)
        cellRenderer = ItemRender()
        addListSelectionListener(this)
    }


    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        jsonObject?.apply {
            val type = this["type"]
            val jsonDataString = jsonObject["jsonDataString"] as? String
            if (type == "getBoxList" && jsonDataString != null) {
                val json = gson.fromJson<Map<String, Any>>(jsonDataString, Map::class.java)
                val data = json["data"] as? ArrayList<*>
                if (data != null) {
                    model = ItemModel(data.map { it.toString() })
                }
            }
        }
    }


    private class ItemModel(list: List<String>) : DefaultListModel<String>() {
        init {
            addAll(list)
        }
    }

    private class ItemRender : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            append(value ?: "")
        }

    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            val projectName = service<AppService>().currentSelectName.get() ?: ""
            DioApiService.getInstance()
                .sendByAnyObject(HiveActionGetKeys(projectName = projectName, boxName = selectedValue))
        }
    }

    override fun dispose() {
        DioApiService.getInstance().removeHandle(this)
        println("hive box list disposed")
    }

}


///盒子里面的 key 列表
private class HiveKeysList(private val boxList: JBList<String>) : JBList<String>(),
    DioApiService.NativeMessageProcessing, ListSelectionListener, Disposable {

    private val gson = DioApiService.getInstance().gson

    init {
        cellRenderer = ItemRender()
        addListSelectionListener(this)
        register()
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        jsonObject?.apply {
            val type = jsonObject["type"]
            val jsonDataString = jsonObject["jsonDataString"] as? String ?: return
            val obj = gson.fromJson(jsonDataString, Map::class.java)
            val data = obj["data"] as? ArrayList<*>

            if (type == "getKeys" && data != null) {
                model = ItemModel(data.map { it.toString() })
            }
        }
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e != null && e.valueIsAdjusting.not() && selectedValue != null) {
            val projectName = service<AppService>().currentSelectName.get() ?: ""
            val boxName = boxList.selectedValue
            DioApiService.getInstance().sendByAnyObject(
                HiveActionGetValue(
                    projectName = projectName, boxName = boxName, key = selectedValue
                )
            )
        }

    }


    private class ItemModel(list: List<String>) : DefaultListModel<String>() {
        init {
            addAll(list)
        }
    }

    private class ItemRender : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean
        ) {
            append(value ?: "")
        }

    }

    override fun dispose() {
        removeMessageProcess()
    }

}