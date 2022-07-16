
import org.slf4j.LoggerFactory
import shop.itbug.fluttercheckversionx.myjsonview.gui.GuiConstants
import shop.itbug.fluttercheckversionx.myjsonview.json.gui.JsonViewerPanel
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.SwingUtilities

class MainGui {
    private var mainFrame: JFrame? = null
    private var jsonMenu: JButton? = null
    private fun setVisible() {
        mainFrame!!.isVisible = true
        jsonMenu!!.doClick()
    }

    private fun init() {
        LOGGER.info("starting the gui in {}*{}", GuiConstants.MAIN_GUI_WIDTH, GuiConstants.MAIN_GUI_HEIGHT)
        mainFrame = JFrame(GuiConstants.MAIN_GUI_TITLE)
        mainFrame!!.setSize(GuiConstants.MAIN_GUI_WIDTH, GuiConstants.MAIN_GUI_HEIGHT)
        mainFrame!!.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        mainFrame!!.isResizable = true
        mainFrame!!.isLocationByPlatform = true
        mainFrame!!.layout = BorderLayout()
        val menuBar = JMenuBar()
        menuBar.add(
            getJMenu(
                "JSON Viewer",
                'J',
                "A JSON Formatter",
                JsonViewerPanel.instance
            ).also { jsonMenu = it })
        mainFrame!!.jMenuBar = menuBar
    }

    private fun getJMenu(title: String, mnemonic: Char, toolTip: String, panel: AbstractPanel): JButton {
        val button = JButton(title)
        button.setMnemonic(mnemonic)
        button.toolTipText = toolTip
        button.addActionListener {
            val contentPane = mainFrame!!.contentPane
            if (contentPane.components.isEmpty() || contentPane.getComponent(0) !== panel) {
                resetMainGui()
                mainFrame!!.contentPane.add(panel)
                refreshMainGui()
            }
        }
        return button
    }

    private fun resetMainGui() {
        mainFrame!!.contentPane.removeAll()
    }

    private fun refreshMainGui() {
        mainFrame!!.revalidate()
        mainFrame!!.repaint()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainGui::class.java)
        @JvmStatic
        fun main(args: Array<String>) {
            val mainGui = MainGui()
            mainGui.init()
            SwingUtilities.invokeLater { mainGui.setVisible() }
        }
    }
}