package shop.itbug.flutterx.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.util.treeView.PresentableNodeDescriptor.ColoredFragment
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.icons.MyIcons
import javax.swing.Icon

private const val PUBSPEC_FILE_NAME = "pubspec.yaml"

class FlutterPlatformDirectoryDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        if (!isFlutterPlatformDirectory(node, file)) return
        if (node.project?.let { PluginConfig.getState(it).showFlutterPlatformDirectoryIcons } != false) {
            platformIcon(file.name)?.let(data::setIcon)
        }
        makeDirectoryNameBold(data, file.name)
    }

    private fun isFlutterPlatformDirectory(node: ProjectViewNode<*>, file: VirtualFile): Boolean {
        if (!file.isDirectory) return false
        if (platformIcon(file.name) == null) return false
        return isFlutterProjectRootChild(node, file)
    }

    private fun platformIcon(directoryName: String): Icon? = when (directoryName) {
        "web" -> MyIcons.platformWeb
        "windows" -> MyIcons.platformWindows
        "linux" -> MyIcons.platformLinux
        "macos" -> MyIcons.platformMacos
        "android" -> MyIcons.platformAndroid
        "ios" -> MyIcons.platformIos
        else -> null
    }

    private fun isFlutterProjectRootChild(node: ProjectViewNode<*>, file: VirtualFile): Boolean {
        val root = node.project?.guessProjectDir() ?: return false
        return file.parent == root && root.findChild(PUBSPEC_FILE_NAME) != null
    }

    private fun makeDirectoryNameBold(data: PresentationData, directoryName: String) {
        if (data.coloredText.isEmpty()) {
            data.addText(directoryName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            return
        }

        val currentText = data.coloredText.toList()
        data.clearText()
        currentText.forEach { fragment ->
            val attributes = if (fragment.text == directoryName) {
                SimpleTextAttributes.merge(fragment.attributes, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            } else {
                fragment.attributes
            }
            data.addText(ColoredFragment(fragment.text, fragment.toolTip, attributes))
        }
    }
}
