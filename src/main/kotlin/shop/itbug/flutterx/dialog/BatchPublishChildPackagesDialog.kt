package shop.itbug.flutterx.dialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.ChildPubPackage
import shop.itbug.flutterx.util.toastWithError
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants

data class BatchPublishPackageRequest(
    val packageInfo: ChildPubPackage,
    val version: String,
    val changelog: String
)

data class BatchPublishRequest(
    val includePublishDate: Boolean,
    val packages: List<BatchPublishPackageRequest>
)

class BatchPublishChildPackagesDialog(
    private val currentProject: Project,
    private val rootDirectory: VirtualFile,
    private val packages: List<ChildPubPackage>,
    private val skippedPackages: Int
) : DialogWrapper(currentProject, true) {

    companion object {
        private const val PACKAGE_COLUMN_WIDTH = 220
        private const val CURRENT_COLUMN_WIDTH = 90
        private const val VERSION_COLUMN_WIDTH = 170
        private const val PUBLISH_COLUMN_WIDTH = 84
        private const val BULK_VERSION_WIDTH = 220
        private const val BULK_ACTION_WIDTH = 160
        private const val COLUMN_GAP = 12
    }

    private val batchVersionField = JBTextField().apply {
        putClientProperty("JTextField.placeholderText", PluginBundle.get("batch_publish_child_packages_version_placeholder"))
    }
    private val batchChangelogArea = JBTextArea().apply {
        rows = 4
        lineWrap = true
        wrapStyleWord = true
        putClientProperty(
            "JTextArea.placeholderText",
            PluginBundle.get("batch_publish_child_packages_changelog_placeholder")
        )
    }
    private val includePublishDateCheckbox =
        JBCheckBox(PluginBundle.get("batch_publish_child_packages_include_publish_date"), false).apply {
            isOpaque = false
        }
    private val packageRows = packages.map { PackagePublishRow(it) }
    private var publishRequest: BatchPublishRequest? = null

    private val previewAction = object : DialogWrapperAction(
        PluginBundle.get("batch_publish_child_packages_preview_button")
    ) {
        override fun doAction(e: ActionEvent?) {
            val request = collectPublishRequest(showErrors = true) ?: return
            CommandOutputDialog(
                currentProject,
                PluginBundle.get("batch_publish_child_packages_preview_title"),
                buildPreviewText(request)
            ).show()
        }
    }

    init {
        title = PluginBundle.get("batch_publish_child_packages_dialog_title", rootDirectory.name)
        setOKButtonText(PluginBundle.get("batch_publish_child_packages_publish_button"))
        isResizable = true
        init()
    }

    fun getPublishRequest(): BatchPublishRequest? = publishRequest

    override fun createLeftSideActions(): Array<Action> = arrayOf(previewAction)

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(16)
            add(buildContentPanel(), BorderLayout.CENTER)
            preferredSize = JBUI.size(1020, 760)
        }
    }

    override fun doOKAction() {
        publishRequest = collectPublishRequest(showErrors = true) ?: return
        super.doOKAction()
    }

    private fun buildContentPanel(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(buildHeaderPanel().leftAligned())
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(buildBulkApplyPanel().leftAligned())
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(buildPackagesTablePanel().leftAligned())
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(buildHintPanel().leftAligned())
        }.leftAligned()
    }

    private fun buildHeaderPanel(): JComponent {
        val titleLabel = JBLabel(PluginBundle.get("batch_publish_child_packages_header_title")).apply {
            font = font.deriveFont(Font.BOLD, font.size2D + 5f)
        }
        val subtitleText = buildString {
            append(
                PluginBundle.get(
                    "batch_publish_child_packages_detected_summary",
                    packages.size.toString(),
                    rootDirectory.name
                )
            )
            if (skippedPackages > 0) {
                append("  ")
                append(PluginBundle.get("batch_publish_child_packages_skipped_none_inline", skippedPackages.toString()))
            }
        }
        val subtitleLabel = createMutedLabel(subtitleText)

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    add(titleLabel.leftAligned())
                    add(Box.createVerticalStrut(JBUI.scale(6)))
                    add(subtitleLabel.leftAligned())
                }.leftAligned(),
                BorderLayout.WEST
            )
        }.leftAligned()
    }

    private fun buildBulkApplyPanel(): JComponent {
        val applyButton = JButton(PluginBundle.get("batch_publish_child_packages_apply_selected")).apply {
            addActionListener { applyBatchValues() }
        }

        val bulkFieldsRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false

            add(
                createFixedColumn(
                    createFieldPanel(
                        PluginBundle.get("batch_publish_child_packages_version_label"),
                        batchVersionField
                    ),
                    BULK_VERSION_WIDTH
                )
            )
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(
                createFlexibleColumn(
                    createFieldPanel(
                        PluginBundle.get("batch_publish_child_packages_shared_changelog_label"),
                        createTextAreaScrollPane(batchChangelogArea, 110)
                    )
                )
            )
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(
                createFixedColumn(
                    JPanel(BorderLayout()).apply {
                        isOpaque = false
                        add(Box.createVerticalStrut(JBUI.scale(24)), BorderLayout.NORTH)
                        add(applyButton, BorderLayout.CENTER)
                    }.leftAligned(),
                    BULK_ACTION_WIDTH
                )
            )
        }.leftAligned()

        val footerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(createMutedLabel(PluginBundle.get("batch_publish_child_packages_markdown_hint")), BorderLayout.WEST)
            add(includePublishDateCheckbox, BorderLayout.EAST)
        }.leftAligned()

        return createCardPanel().apply {
            add(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    add(createSectionTitle(PluginBundle.get("batch_publish_child_packages_batch_settings")))
                    add(Box.createVerticalStrut(JBUI.scale(4)))
                    add(createMutedLabel(PluginBundle.get("batch_publish_child_packages_order_hint")).leftAligned())
                    add(Box.createVerticalStrut(JBUI.scale(12)))
                    add(bulkFieldsRow)
                    add(Box.createVerticalStrut(JBUI.scale(10)))
                    add(footerPanel)
                }.leftAligned(),
                BorderLayout.CENTER
            )
        }
    }

    private fun buildPackagesTablePanel(): JComponent {
        val rowsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            packageRows.forEachIndexed { index, row ->
                add(row.leftAligned())
                if (index != packageRows.lastIndex) {
                    add(createRowDivider().leftAligned())
                }
            }
        }.leftAligned()

        return createCardPanel().apply {
            add(createHeaderRow(), BorderLayout.NORTH)
            add(
                JBScrollPane(rowsPanel).apply {
                    border = JBUI.Borders.empty()
                    preferredSize = JBUI.size(920, 430)
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    viewport.isOpaque = false
                    isOpaque = false
                },
                BorderLayout.CENTER
            )
        }
    }

    private fun buildHintPanel(): JComponent {
        val hintText = buildString {
            append(PluginBundle.get("batch_publish_child_packages_hint", rootDirectory.name))
            if (skippedPackages > 0) {
                append(" ")
                append(PluginBundle.get("batch_publish_child_packages_skipped_none_inline", skippedPackages.toString()))
            }
        }

        return JPanel(BorderLayout(10, 0)).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor(0xB9D4FF, 0x425A75), 1),
                JBUI.Borders.empty(10, 12)
            )
            background = JBColor(0xF4F8FF, 0x2F3640)
            add(JBLabel(AllIcons.General.Information), BorderLayout.WEST)
            add(createMutedLabel(hintText), BorderLayout.CENTER)
        }.leftAligned()
    }

    private fun collectPublishRequest(showErrors: Boolean): BatchPublishRequest? {
        val selectedRows = packageRows.filter { it.publishCheckBox.isSelected }
        if (selectedRows.isEmpty()) {
            if (showErrors) {
                currentProject.toastWithError(PluginBundle.get("batch_publish_child_packages_select_at_least_one"))
            }
            return null
        }

        val requests = mutableListOf<BatchPublishPackageRequest>()
        selectedRows.forEach { row ->
            val version = row.versionField.text.trim()
            if (version.isBlank()) {
                if (showErrors) {
                    currentProject.toastWithError(
                        PluginBundle.get("batch_publish_child_packages_version_required", row.packageInfo.name)
                    )
                }
                return null
            }

            val changelog = row.changelogArea.text.trim()
            if (changelog.isBlank()) {
                if (showErrors) {
                    currentProject.toastWithError(
                        PluginBundle.get("batch_publish_child_packages_changelog_required", row.packageInfo.name)
                    )
                }
                return null
            }

            requests += BatchPublishPackageRequest(row.packageInfo, version, changelog)
        }

        return BatchPublishRequest(
            includePublishDate = includePublishDateCheckbox.isSelected,
            packages = requests
        )
    }

    private fun buildPreviewText(request: BatchPublishRequest): String {
        return buildString {
            appendLine(PluginBundle.get("batch_publish_child_packages_header_title"))
            appendLine("root: ${rootDirectory.name}")
            appendLine("packages: ${request.packages.size}")
            appendLine("includePublishDate: ${request.includePublishDate}")
            appendLine()
            request.packages.forEach { item ->
                appendLine("## ${item.packageInfo.name}")
                appendLine("current: ${item.packageInfo.version.ifBlank { "-" }}")
                appendLine("next: ${item.version}")
                appendLine(item.changelog)
                appendLine()
            }
        }
    }

    private fun applyBatchValues() {
        val selectedRows = packageRows.filter { it.publishCheckBox.isSelected }
        if (selectedRows.isEmpty()) {
            currentProject.toastWithError(PluginBundle.get("batch_publish_child_packages_apply_select_first"))
            return
        }

        val batchVersion = batchVersionField.text.trim()
        val batchChangelog = batchChangelogArea.text.trim()
        if (batchVersion.isBlank() && batchChangelog.isBlank()) {
            currentProject.toastWithError(PluginBundle.get("batch_publish_child_packages_apply_empty"))
            return
        }

        selectedRows.forEach { row ->
            if (batchVersion.isNotBlank()) {
                row.versionField.text = batchVersion
            }
            if (batchChangelog.isNotBlank()) {
                row.changelogArea.text = batchChangelog
            }
        }
    }

    private fun createHeaderRow(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLineBottom(JBColor.border()),
                JBUI.Borders.emptyBottom(10)
            )
            isOpaque = false
            add(
                createColumnsRow(
                    createHeaderLabel(PluginBundle.get("batch_publish_child_packages_column_package")),
                    createHeaderLabel(
                        PluginBundle.get("batch_publish_child_packages_column_current"),
                        SwingConstants.CENTER
                    ),
                    createHeaderLabel(PluginBundle.get("batch_publish_child_packages_column_new_version")),
                    createHeaderLabel(PluginBundle.get("batch_publish_child_packages_column_changelog")),
                    createHeaderLabel(
                        PluginBundle.get("batch_publish_child_packages_column_publish"),
                        SwingConstants.CENTER
                    )
                ),
                BorderLayout.CENTER
            )
        }.leftAligned()
    }

    private fun createColumnsRow(
        packageComponent: JComponent,
        currentComponent: JComponent,
        versionComponent: JComponent,
        changelogComponent: JComponent,
        publishComponent: JComponent
    ): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(createFixedColumn(packageComponent, PACKAGE_COLUMN_WIDTH))
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(createFixedColumn(currentComponent, CURRENT_COLUMN_WIDTH))
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(createFixedColumn(versionComponent, VERSION_COLUMN_WIDTH))
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(createFlexibleColumn(changelogComponent))
            add(Box.createHorizontalStrut(JBUI.scale(COLUMN_GAP)))
            add(createFixedColumn(publishComponent, PUBLISH_COLUMN_WIDTH))
        }.leftAligned()
    }

    private fun createFixedColumn(component: JComponent, width: Int): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            preferredSize = JBUI.size(width, component.preferredSize.height.coerceAtLeast(32))
            minimumSize = JBUI.size(width, 0)
            maximumSize = JBUI.size(width, Int.MAX_VALUE)
            add(component, BorderLayout.CENTER)
        }.leftAligned()
    }

    private fun createFlexibleColumn(component: JComponent): JComponent {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            minimumSize = JBUI.size(120, 0)
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
            add(component, BorderLayout.CENTER)
        }.leftAligned()
    }

    private fun createHeaderLabel(text: String, horizontalAlignment: Int = SwingConstants.LEADING): JBLabel {
        return JBLabel(text, horizontalAlignment).apply {
            font = font.deriveFont(Font.BOLD)
        }.leftAligned()
    }

    private fun createSectionTitle(text: String): JComponent {
        return JBLabel(text).apply {
            font = font.deriveFont(Font.BOLD, font.size2D + 1f)
        }.leftAligned()
    }

    private fun createMutedLabel(text: String): JBLabel {
        return JBLabel(text).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        }.leftAligned()
    }

    private fun createCardPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1),
                JBUI.Borders.empty(14)
            )
            background = JBColor.PanelBackground
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }.leftAligned()
    }

    private fun createFieldPanel(label: String, field: JComponent): JPanel {
        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            isOpaque = false
            add(JBLabel(label), BorderLayout.NORTH)
            add(field, BorderLayout.CENTER)
        }.leftAligned()
    }

    private fun createTextAreaScrollPane(textArea: JBTextArea, height: Int): JBScrollPane {
        return JBScrollPane(textArea).apply {
            preferredSize = JBUI.size(420, height)
            minimumSize = JBUI.size(120, height)
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun createRowDivider(): JComponent {
        return JPanel().apply {
            maximumSize = Dimension(Int.MAX_VALUE, 1)
            preferredSize = Dimension(1, 1)
            background = JBColor.border()
        }
    }

    private fun <T : JComponent> T.leftAligned(): T {
        alignmentX = Component.LEFT_ALIGNMENT
        alignmentY = Component.TOP_ALIGNMENT
        return this
    }

    private inner class PackagePublishRow(val packageInfo: ChildPubPackage) : JPanel(BorderLayout()) {
        val publishCheckBox = JBCheckBox("", true).apply {
            isOpaque = false
            horizontalAlignment = SwingConstants.CENTER
        }
        val versionField = JBTextField(packageInfo.version).apply {
            putClientProperty("JTextField.placeholderText", PluginBundle.get("batch_publish_child_packages_version_placeholder"))
        }
        val changelogArea = JBTextArea().apply {
            rows = 3
            lineWrap = true
            wrapStyleWord = true
            putClientProperty(
                "JTextArea.placeholderText",
                PluginBundle.get("batch_publish_child_packages_changelog_placeholder")
            )
        }

        init {
            border = JBUI.Borders.empty(10, 0)
            isOpaque = false

            val packageLabel = JBLabel(packageInfo.name, MyIcons.dartPackageIcon, SwingConstants.LEADING).apply {
                iconTextGap = JBUI.scale(8)
            }

            val currentVersionLabel = createMutedLabel(packageInfo.version.ifBlank { "-" }).apply {
                horizontalAlignment = SwingConstants.CENTER
            }

            add(
                createColumnsRow(
                    packageLabel,
                    currentVersionLabel,
                    versionField,
                    createTextAreaScrollPane(changelogArea, 84),
                    JPanel(BorderLayout()).apply {
                        isOpaque = false
                        add(publishCheckBox, BorderLayout.NORTH)
                    }.leftAligned()
                ),
                BorderLayout.CENTER
            )
        }
    }
}
