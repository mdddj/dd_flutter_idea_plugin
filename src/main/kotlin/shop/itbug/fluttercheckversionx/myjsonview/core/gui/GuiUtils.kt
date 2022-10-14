
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.fife.ui.rtextarea.RTextScrollPane
import shop.itbug.fluttercheckversionx.myjsonview.core.gui.ExtendedTextPane
import java.awt.event.KeyEvent
import java.io.IOException
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.KeyStroke

object GuiUtils {
    fun getScrollTextPane(contentType: String): RTextScrollPane {
        val scrollPane = RTextScrollPane(getTextPane(contentType))
        scrollPane.isFoldIndicatorEnabled = true
        scrollPane.lineNumbersEnabled = true
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        return scrollPane
    }

    private fun getTextPane(contentType: String): ExtendedTextPane {
        val textPane = ExtendedTextPane()
        textPane.syntaxEditingStyle = contentType
        textPane.autoscrolls = true
        return textPane
    }

    fun applyShortcut(component: JComponent?, keyCode: Int, actionKey: String?, abstractAction: AbstractAction?) {
        val im = component!!.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        im.put(KeyStroke.getKeyStroke(keyCode, KeyEvent.CTRL_DOWN_MASK), actionKey)
        val am = component.actionMap
        am.put(actionKey, abstractAction)
    }

    @Throws(IOException::class)
    fun readFile(): String {
        val builder = StringBuilder()
        return builder.toString()
    }

    @Throws(IOException::class)
    fun toPrettyJson(text: String?): String {
        val builder = GsonBuilder()
        builder.setPrettyPrinting().disableHtmlEscaping().serializeNulls().setLenient()
        val gson = builder.create()
        val jsonObj = gson.fromJson(text, Any::class.java)
        return gson.toJson(jsonObj)
    }

    fun toSimpleJson(text: String?): String {
        val gson = Gson()
        val jsonObj = gson.fromJson(text, Any::class.java)
        return gson.toJson(jsonObj)
    }

    @Throws(IOException::class)
    fun validateJson(text: String?) {
        val mapper = ObjectMapper()
        mapper.readTree(text)
    }

    fun escapeHtmlEntites(str: String): String {
        return str.replace("&lt;", "<").replace("&gt;", ">")
    }
}