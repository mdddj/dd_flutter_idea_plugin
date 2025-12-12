package shop.itbug.flutterx.icons

import com.intellij.openapi.util.IconLoader
import org.jetbrains.jewel.ui.icon.IntelliJIconKey
import org.jetbrains.jewel.ui.icon.PathIconKey

object MyIcons {
    var dartPackageIcon = IconLoader.getIcon("/icons/dart.svg", MyIcons::class.java)
    var dartPluginIcon = IconLoader.getIcon("/icons/pluginIcon.svg", MyIcons::class.java)
    var diandianLogoIcon = IconLoader.getIcon("/icons/diandian.svg", MyIcons::class.java)
    val params = IconLoader.getIcon("icons/params.svg", MyIcons::class.java)
    var flutter = IconLoader.getIcon("/icons/flutter.svg", MyIcons::class.java)
    var add = IconLoader.getIcon("/icons/add.svg", MyIcons::class.java)
    var setting = IconLoader.getIcon("/icons/setting.svg", MyIcons::class.java)
    var freezed = IconLoader.getIcon("/icons/freezed.svg", MyIcons::class.java)
    val score = IconLoader.getIcon("/icons/score.svg", MyIcons::class.java)
    val androidStudio = IconLoader.getIcon("/icons/as.svg", MyIcons::class.java)
    val xcode = IconLoader.getIcon("/icons/xcode.svg", MyIcons::class.java)
    val language = IconLoader.getIcon("/icons/language.svg", MyIcons::class.java)
    val image = IconLoader.getIcon("/icons/image.svg", MyIcons::class.java)
    val imageIconPath = PathIconKey("/icons/image.svg",MyIcons::class.java)

    val moreHorizontal = IconLoader.getIcon("/icons/moreHorizontal.svg", MyIcons::class.java)

    /// compose
    val download = IntelliJIconKey("/icons/new/download.svg","/icons/new/download.svg", MyIcons::class.java)
}


