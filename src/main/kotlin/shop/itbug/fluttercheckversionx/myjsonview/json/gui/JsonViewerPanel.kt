package shop.itbug.fluttercheckversionx.myjsonview.json.gui

import AbstractPanel
import com.fasterxml.jackson.core.JsonProcessingException
import shop.itbug.fluttercheckversionx.myjsonview.core.gui.ExtendedTextPane
import shop.itbug.fluttercheckversionx.myjsonview.gui.GuiConstants
import com.google.gson.JsonSyntaxException
import com.intellij.util.ui.UIUtil
import org.apache.commons.lang3.StringUtils
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.SearchContext
import org.fife.ui.rtextarea.SearchEngine
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class JsonViewerPanel : AbstractPanel() {
    private val jsonIndicator = JLabel()
    private val scrollPane = GuiUtils.getScrollTextPane(SyntaxConstants.SYNTAX_STYLE_JSON)
    private  var textPane: ExtendedTextPane = scrollPane.textArea as ExtendedTextPane
    private var searchField: JTextField? = null
    private val timer: Timer
    private val msgLabel = JLabel()
    private val msgPanel = JPanel()
    private var lastDirectory: File? = null

    init {
        init()
        timer = Timer(GuiConstants.DEFAULT_DELAY_MS) { validateJson() }
        timer.isRepeats = false
    }

    private fun init() {
        border = BorderFactory.createEmptyBorder()
        textPane.border = BorderFactory.createEmptyBorder()
        textPane.background = UIUtil.getPanelBackground()
        size = maximumSize
        layout = BorderLayout()
        val settingPanel = JToolBar()
        settingPanel.add(jsonIndicator)
        settingPanel.add(loadFileButton)
        settingPanel.add(formatButton)
        settingPanel.add(deformatButton)
        settingPanel.add(JLabel("Search"))
        settingPanel.add(getSearchField().also { searchField = it })
        settingPanel.add(msgLabel)
        msgPanel.add(msgLabel)
        val topPanel = JPanel(BorderLayout())
        topPanel.add(settingPanel, BorderLayout.NORTH)
        topPanel.add(msgPanel, BorderLayout.CENTER)
        add(topPanel, BorderLayout.NORTH)
        textPane.isCodeFoldingEnabled = true
        textPane.highlightCurrentLine = true
        textPane.isAutoIndentEnabled = true
        textPane.hyperlinksEnabled = true
        textPane.isBracketMatchingEnabled = true
        textPane.paintMatchedBracketPair = true
        GuiUtils.applyShortcut(textPane, KeyEvent.VK_L, "lineNumber", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                try {
                    textPane.caretLineNumber = JOptionPane.showInputDialog(
                        this@JsonViewerPanel, String.format("跳转到行 (1, %d)", textPane.lineCount),
                        "行跳转", JOptionPane.PLAIN_MESSAGE, null, null,
                        (textPane.caretLineNumber + 1).toString()
                    ) as Int
                } catch (ex: NumberFormatException) {
                    UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
                } catch (ex: Exception) {
                    UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
                }
            }
        })
        textPane.document.addDocumentListener(object : DocumentListener {
            override fun removeUpdate(e: DocumentEvent) {
                timer.restart()
            }

            override fun insertUpdate(e: DocumentEvent) {
                timer.restart()
            }

            override fun changedUpdate(e: DocumentEvent) {}
        })
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun loadFile() {
        val fileChooser = JFileChooser(lastDirectory)
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            lastDirectory = fileChooser.currentDirectory
            readFile()
        }
    }

    private fun validateJson() {
        val text = textPane.text
        if (StringUtils.isBlank(text)) {
            jsonIndicator.icon = null
            msgPanel.isVisible = false
            return
        }
        try {
            GuiUtils.validateJson(text)
            msgPanel.isVisible = false
        } catch (e: Exception) {
            popup(e)
        }
    }

    ///更改文本
    fun changeText(text:String) {
        textPane.text = text
        toSimpleJson()
        toPrettyJson()
    }


    private fun toSimpleJson() {
        val text = textPane.text
        if (StringUtils.isBlank(text)) {
            return
        }
        try {
            textPane.text = GuiUtils.toSimpleJson(text)
            msgPanel.isVisible = false
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun toPrettyJson() {
        val text = textPane.text
        if (StringUtils.isBlank(text)) {
            return
        }
        try {
            textPane.text = GuiUtils.toPrettyJson(text)
            msgPanel.isVisible = false
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun readFile() {
        try {
            textPane.text = GuiUtils.readFile()
        } catch (e: Exception) {
            popup(e)
        }
    }

    private fun findInJson() {
        val findText = searchField!!.text
        val context = SearchContext(findText)
        if (!SearchEngine.find(textPane, context).wasFound()) {
            UIManager.getLookAndFeel().provideErrorFeedback(this@JsonViewerPanel)
        }
        RTextArea.setSelectedOccurrenceText(findText)
    }

    private val loadFileButton: JButton
        get() {
            val loadFileButton = JButton("Open File")
            loadFileButton.toolTipText = "loads contents from file"
            GuiUtils.applyShortcut(loadFileButton, KeyEvent.VK_O, "Open", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    loadFile()
                }
            })
            loadFileButton.addActionListener { loadFile() }
            return loadFileButton
        }
    private val formatButton: JButton
        get() {
            val formatButton = JButton("Format")
            formatButton.toolTipText = "formats the input provided"
            GuiUtils.applyShortcut(formatButton, KeyEvent.VK_Q, "Format", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    toPrettyJson()
                }
            })
            formatButton.addActionListener { toPrettyJson() }
            return formatButton
        }
    private val deformatButton: JButton
        get() {
            val deformatButton = JButton("DeFormat")
            deformatButton.toolTipText = "compresses json, should be used to send compressed data over networks"
            GuiUtils.applyShortcut(deformatButton, KeyEvent.VK_W, "Deformat", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    toSimpleJson()
                }
            })
            deformatButton.addActionListener { toSimpleJson() }
            return deformatButton
        }

    private fun getSearchField(): JTextField {
        val searchField = JTextField(10)
        searchField.toolTipText = "CTRL+K (fwd) CTRL+SHIFT+K (bkd)"
        GuiUtils.applyShortcut(searchField, KeyEvent.VK_F, "Find", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                searchField.requestFocusInWindow()
            }
        })
        searchField.addActionListener { findInJson() }
        return searchField
    }

    private fun popup(e: Exception) {
        val filteredMsg = StringBuilder()
        when (e) {
            is JsonProcessingException -> {
                filteredMsg.append(e.originalMessage)
                val location = e.location
                val locationStr = String.format(" at line %d col %d", location.lineNr, location.columnNr)
                filteredMsg.append(locationStr)
            }
            is JsonSyntaxException -> {
                val msg = StringUtils.substringAfter(e.message, "Exception: ")
                filteredMsg.append(msg)
            }
            else -> {
                filteredMsg.append(e.message)
            }
        }
        msgLabel.text = filteredMsg.toString()
        msgLabel.foreground = Color.RED
        msgPanel.isVisible = true
    }

    companion object {
        val instance = JsonViewerPanel()
    }
}