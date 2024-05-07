package shop.itbug.fluttercheckversionx.dialog

import PluginVersionModel
import cn.hutool.http.HttpRequest
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FlutterPluginType
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener


data class PubSearchResult(
    val packages: List<Package>,
    val next: String
)

data class Package(
    val `package`: String
)


class SearchDialog(val project: Project) : DialogWrapper(project) {


    private var selectedModel: Package? = null
    private var selectLabel: JLabel = JLabel()


    private var versionSelect: VersionSelect = VersionSelect()
    private var bottomPanel = Box.createHorizontalBox()
    private val resultList = SearchListResultShow {
        myOKAction.isEnabled = false
        bottomPanel.add(versionSelect)
        selectedModel = it
        selectLabel.text = it.`package` + ":"
        versionSelect.doRequest(it.`package`)
        myOKAction.isEnabled = true

    }

    //项目所有插件
    private var allPlugins = emptyList<String>()

    init {
        title = PluginBundle.get("search.pub.plugin")
        init()
        getAllPlugins()
        setOKButtonText(PluginBundle.get("add"))
        setCancelButtonText(PluginBundle.get("cancel"))
        myOKAction.isEnabled = false
    }


    override fun doOKAction() {
        doInset()
        super.doOKAction()
    }


    //执行插入
    private fun doInset() {
        selectedModel?.let {
            MyPsiElementUtil.insertPluginToPubspecFile(
                project,
                it.`package`,
                versionSelect.item,
                FlutterPluginType.Dependencies
            )
        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(600, 380)
    }


    override fun isOKActionEnabled(): Boolean {
        return false
    }

    private fun getAllPlugins() {
        allPlugins = MyPsiElementUtil.getAllPlugins(project)
    }

    override fun createCenterPanel(): JComponent {
        val corePanel = BorderLayoutPanel()
        corePanel.preferredSize = Dimension(500, 300)
        corePanel.addToTop(MySearchField {
            resultList.model = ResultModel(it.packages)
        })
        corePanel.addToCenter(JBScrollPane(resultList))
        bottomPanel.add(selectLabel)
        corePanel.addToBottom(bottomPanel)
        return corePanel
    }

}


/**
 * 获取输入的搜索包
 */
typealias SearchResultHandle = (obj: PubSearchResult) -> Unit

class MySearchField(val handle: SearchResultHandle) : JPanel() {

    private val searchTextField = JBTextField()

    private val searchButton = JButton(PluginBundle.get("search"))

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(JBLabel("${PluginBundle.get("name.plugin")}:"))
        add(searchTextField)
        add(searchButton)
        searchButton.addActionListener {
            doSearch()
        }
        searchTextField.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(e: KeyEvent?) {
                if (e?.keyChar == KeyEvent.VK_ENTER.toChar()) {
                    doSearch()
                }
                super.keyTyped(e)
            }
        })
    }

    //执行搜索
    private fun doSearch() {
        searchButton.label = "${PluginBundle.get("search")}..."
        val keyWorlds = searchTextField.text
        try {
            val response = HttpRequest.get("https://pub.dartlang.org/api/search?q=${keyWorlds}").execute()
            val result = Gson().fromJson(response.body(), PubSearchResult::class.java)
            handle(result)
        } catch (_: Exception) {
        }
        searchButton.label = PluginBundle.get("search")
    }

}


typealias DoSelectChange = (model: Package) -> Unit

class SearchListResultShow(val doSelect: DoSelectChange) : JBList<Package>(), ListSelectionListener {

    init {
        cellRenderer = ReultItemRender()
        setEmptyText(PluginBundle.get("empty"))
        addListSelectionListener(this)
    }

    override fun valueChanged(e: ListSelectionEvent?) {
        if (e?.valueIsAdjusting == false) {
            if (leadSelectionIndex != -1) {
                val selectedModel = model.getElementAt(leadSelectionIndex)
                doSelect(selectedModel)
            }
        }
    }
}


class ResultModel(private val packages: List<Package>) : DefaultListModel<Package>() {
    override fun get(index: Int): Package {
        return packages[index]
    }

    override fun getSize(): Int {
        return packages.size
    }

    override fun getElementAt(index: Int): Package {
        return packages[index]
    }
}

class ReultItemRender : ListCellRenderer<Package> {
    override fun getListCellRendererComponent(
        list: JList<out Package>?,
        value: Package?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return JLabel(value?.`package`)
    }
}


class VersionSelect : ComboBox<String>() {

    init {
        isEnabled = false
        model = VersionSelectModel(emptyList())
    }

    fun doRequest(pluginName: String) {

        model = VersionSelectModel(emptyList())
        try {
            val response = HttpRequest.get("https://pub.dartlang.org/packages/$pluginName.json").execute()
            val result = Gson().fromJson(response.body(), PluginVersionModel::class.java)
            model = VersionSelectModel(versions = result.versions)
            isEnabled = true
            model.selectedItem = result.versions.first()
        } catch (e: Exception) {
            println("搜索失败")
        }
    }
}

class VersionSelectModel(val versions: List<String>) : DefaultComboBoxModel<String>() {
    override fun getSize(): Int {
        return versions.size
    }

    override fun getElementAt(index: Int) = versions.get(index)

    override fun getIndexOf(anObject: Any?) = versions.indexOf(anObject as String)


}