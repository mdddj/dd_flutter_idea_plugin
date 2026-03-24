package icons

import com.intellij.openapi.util.IconLoader
import org.jetbrains.jewel.ui.icon.PathIconKey
import javax.swing.Icon

object MyImages {

    @JvmField
    val wx: Icon = load("/images/wx.png")


    @JvmField
    val ignore: Icon = load("/icons/ignore.svg")



    @JvmStatic
    fun load(path: String): Icon {
        return IconLoader.getIcon(path, MyImages::class.java)
    }

    val wxDs = PathIconKey("images/wx.png", MyImages::class.java)
}