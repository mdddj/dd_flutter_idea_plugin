package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MyImages {

    @JvmField
    val wx : Icon = load("/images/wx.png")

    @JvmField
    val test : Icon = load("/images/test.png")





    @JvmStatic
    fun load(path: String): Icon {
        return IconLoader.getIcon(path, MyImages::class.java)
    }
}