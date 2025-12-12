package shop.itbug.flutterx.util

import com.intellij.ide.ui.UISettingsUtils
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import java.awt.Font
import kotlin.math.roundToInt

object MyFontUtil {

    /**
     * 获取编辑器的默认字体
     * 支持缩放大小
     */
    fun getDefaultFont(): Font {
        val scheme = EditorColorsManager.getInstance().globalScheme
        var font = scheme.getFont(EditorFontType.PLAIN)
        val scale = getScale()
        font = Font(font.name, font.style, (scale * font.size).roundToInt())
        return font
    }

    /**
     * 获取编辑器缩放
     */
    fun getScale(): Float = UISettingsUtils.getInstance().currentIdeScale


    /**
     * 获取dio接口紧凑模式下method标签的宽度
     */
    fun getRequestLayoutRenderMethodMinWidth(): Int {
        return 48 * getScale().roundToInt()
    }

}