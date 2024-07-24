package shop.itbug.fluttercheckversionx.hive.component

import com.intellij.openapi.components.service
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
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.hive.model.HiveActionGetKeys
import shop.itbug.fluttercheckversionx.hive.model.HiveActionGetValue
import shop.itbug.fluttercheckversionx.socket.service.AppService
import shop.itbug.fluttercheckversionx.socket.service.DioApiService
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

///盒子列表
class HiveBoxListComponent : OnePixelSplitter() {


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
    }
}

///盒子列表
class HiveBoxList : JBList<String>(), DioApiService.NativeMessageProcessing, ListSelectionListener {

    init {
        DioApiService.getInstance().addHandle(this)
        cellRenderer = ItemRender()
        addListSelectionListener(this)
    }


    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        jsonObject?.apply {
            val type = this["type"]
            if (type == "getBoxList") {
                val data = jsonObject["data"]
                data?.let {
                    if (data is ArrayList<*>) {
                        model = ItemModel(data.map { it.toString() })
                    }
                }

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
            DioApiService.getInstance()
                .sendByAnyObject(HiveActionGetKeys(projectName = projectName, boxName = selectedValue))
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

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        jsonObject?.apply {
            val type = jsonObject["type"]
            if (type == "getKeys") {
                val data = jsonObject["data"]
                if (data is ArrayList<*>) {
                    model = ItemModel(data.map { it.toString() })
                }
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