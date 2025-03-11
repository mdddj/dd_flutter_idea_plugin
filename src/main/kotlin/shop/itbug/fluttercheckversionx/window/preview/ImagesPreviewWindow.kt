package shop.itbug.fluttercheckversionx.window.preview

import com.intellij.ide.actions.CopyReferencePopup
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.WrapLayout
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.actions.imagePreview.toolbar.FlutterAssetsPreviewPanelToolbarActionGroup
import shop.itbug.fluttercheckversionx.actions.imagePreview.toolbar.PreviewSearchTextField
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.ImageFileUtil
import shop.itbug.fluttercheckversionx.util.calculateAspectRatio
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * 资产图片预览窗口
 */
class ImagesPreviewWindow(val project: Project, val toolWindow: ToolWindow) : BorderLayoutPanel(), UiDataProvider,
    Disposable {

    val pluginConfig = PluginConfig.getState(project)
    private val assetsDirector: VirtualFile?
        get() = ApplicationManager.getApplication().executeOnPooledThread<VirtualFile?> {
            if (pluginConfig.assetDirectory == null) {
                return@executeOnPooledThread null
            }
            return@executeOnPooledThread LocalFileSystem.getInstance().findFileByPath(pluginConfig.assetDirectory!!)
        }.get()
    private val visitor = FileVisitor()
    val panel = ImagePanel()

    val searchField = PreviewSearchTextField(onSearchChanged)
    val fileComps = mutableSetOf<AssetFileLayout>()


    val toolbar = ActionManager.getInstance().createActionToolbar(
        "Assets Preview Toolbar",
        FlutterAssetsPreviewPanelToolbarActionGroup.getActionGroup(), true
    ).apply {
        targetComponent = toolWindow.component
    }

    val scp = panel.scroll()
    val alarm = Alarm(this)


    val onSearchChanged: (String) -> Unit
        get() = { str ->
            alarm.cancelAllRequests()
            alarm.addRequest({
                panel.removeAll()
                if (str.isNotEmpty()) {
                    fileComps.forEach {
                        val fileName = it.file.nameWithoutExtension
                        if (StringUtil.toLowerCase(fileName).contains(StringUtil.toLowerCase(str))) {
                            panel.add(it)
                        }
                    }
                } else {
                    fileComps.map(panel::add)
                }
                ApplicationManager.getApplication().invokeLater {
                    alarm.addRequest({
                        panel.repaint()
                        panel.updateUI()
                    }, 334)
                }
            }, 330)

        }

    init {

        addToTop(BorderLayoutPanel().apply {
            addToCenter(searchField)
            addToRight(toolbar.component)
        })
        addToCenter(scp)

        SwingUtilities.invokeLater {
            if (pluginConfig.enableAssetsPreviewAction) {
                startLoadAssets()
            } else {
                addToCenter(JBLabel(PluginBundle.get("action_not_open_and_enable")).apply {
                    horizontalAlignment = SwingConstants.CENTER
                })
            }
        }

        addNullDirectoryTips()
    }

    private fun addNullDirectoryTips() {
        val dir = pluginConfig.assetDirectory
        if (dir.isNullOrBlank()) {
            addToCenter(JBLabel("Assets Preview Directory Not Found\nPlease set in the settings").apply {
                horizontalAlignment = SwingConstants.CENTER
            })
        }
    }


    private val onImageAssetVisitor: (VirtualFile) -> Unit
        get() = {
            //获取到的图片文件，添加到网格里面去
            panel.addImageAsset(it)
        }

    private fun startLoadAssets() {
        assetsDirector?.let {
            VfsUtil.visitChildrenRecursively(it, visitor)
        }

    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[KEY] = this
    }

    companion object {
        val KEY = DataKey.create<ImagesPreviewWindow>("FlutterXAssetsPreviewWindow")
    }


    fun refreshItems() {
        panel.removeAll()
        startLoadAssets()
        println("刷新")
    }

    override fun dispose() {

    }

    // 访问图片
    inner class FileVisitor() : VirtualFileVisitor<VirtualFile>() {
        override fun visitFileEx(file: VirtualFile): Result {
            if (ImageFileUtil.isImageFile(file)) {
                this@ImagesPreviewWindow.onImageAssetVisitor(file)
            }
            return CONTINUE
        }

    }

    // 面板 ui
    inner class ImagePanel() : JBPanel<ImagePanel>(WaterfallLayout()) {

        //添加图片资产
        fun addImageAsset(asset: VirtualFile) {
            val item = AssetFileLayout(project, asset)
            fileComps.add(item)
            add(item)
        }
    }
}

class AssetFileLayout(project: Project, val file: VirtualFile) : BorderLayoutPanel(), UiDataProvider {
    val pluginConfig = PluginConfig.getState(project)
    val image: ImageIcon
        get() = ApplicationManager.getApplication().executeOnPooledThread<ImageIcon> { ImageFileUtil.getIcon(file) }
            .get()
    val imageItemSize = pluginConfig.assetsPreviewImageSize
    val resizeIcon = ImageFileUtil.resizeImageIconCover(image, imageItemSize, imageItemSize)
    val imageSize = ImageFileUtil.getSize(file, project)
    val nameLabel = JBLabel(
        "${file.name} (${imageSize.width}x${imageSize.height},${
            calculateAspectRatio(
                imageSize.width, imageSize.height
            )
        })", UIUtil.ComponentStyle.SMALL
    ).apply {
        horizontalAlignment = SwingConstants.CENTER
        border = JBUI.Borders.empty(5, 0)
    }
    private var highlightEnabled = false

    private val imageLabel = JBLabel(resizeIcon).apply {

    }

    init {
        addToCenter(imageLabel)
        addToBottom(nameLabel)
        installPopup(this)
        addMouseListener(object : MouseAdapter() {

            // 右键点击保持高亮
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    highlightEnabled = true
                    updateBorder()
                }
            }

            override fun mouseEntered(e: MouseEvent) {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent) {
                cursor = Cursor.getDefaultCursor()
            }
        })
        border = itemBorder
    }

    private fun installPopup(comp: JComponent) {
        PopupHandler.installPopupMenu(
            comp,
            ActionManager.getInstance().getAction("FlutterXAssetsImagePreviewPopup") as CopyReferencePopup,
            "Copy With",
            object : PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                }

                override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                    highlightEnabled = false
                    updateBorder()
                }

                override fun popupMenuCanceled(e: PopupMenuEvent?) {
                    highlightEnabled = false
                    updateBorder()
                }

            })
    }

    override fun uiDataSnapshot(sink: DataSink) {
        sink[CommonDataKeys.VIRTUAL_FILE] = file
    }

    private fun updateBorder() {
        border = if (highlightEnabled) highlightBorder else itemBorder
        repaint() // 触发重绘
    }


}

//瀑布流布局
private class WaterfallLayout : WrapLayout() {
    private val gaps = JBUIScale.scale(5)
    private val columnWidth = JBUIScale.scale(200)

    override fun layoutContainer(target: Container) {
        val width = target.width
        val columnCount = (width / (columnWidth + gaps)).coerceAtLeast(1)
        val heights = IntArray(columnCount) { 0 }

        target.components.forEachIndexed { index, comp ->
            val column = index % columnCount
            val x = column * (columnWidth + gaps)
            val y = heights[column]
            comp.setBounds(x, y, columnWidth, comp.preferredSize.height)
            heights[column] += comp.height + gaps
        }
    }

    override fun preferredLayoutSize(target: Container): Dimension {
        val widths = target.width
        val columnCount = (widths / (columnWidth + gaps)).coerceAtLeast(1)
        val heights = IntArray(columnCount) { 0 }

        target.components.forEachIndexed { index, comp ->
            val column = index % columnCount
            heights[column] += comp.preferredSize.height + gaps
        }

        val maxHeight = heights.maxOrNull() ?: 0
        return Dimension(widths, maxHeight)
    }

}

private val itemBorder = BorderFactory.createCompoundBorder(
    JBUI.Borders.customLine(JBColor.border(), 1), JBUI.Borders.empty(5)
)

private val highlightBorder = JBUI.Borders.compound(
    JBUI.Borders.customLine(JBColor.blue, 1),// 高亮颜色
    JBUI.Borders.empty(5)
)

