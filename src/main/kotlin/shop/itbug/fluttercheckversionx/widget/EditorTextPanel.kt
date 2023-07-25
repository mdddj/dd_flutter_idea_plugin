package shop.itbug.fluttercheckversionx.widget

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ex.AbstractDelegatingToRootTraversalPolicy
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.ComponentUtil
import com.intellij.ui.EditorSettingsProvider
import com.intellij.ui.EditorTextComponent
import com.intellij.ui.TextAccessor
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.IJSwingUtilities
import com.intellij.util.LocalTimeCounter
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.*
import javax.swing.CellRendererPane
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.UIManager


open class EditorTextPanel @JvmOverloads constructor(
    document: Document?,
    project: Project?,
    fileType: FileType?,
    isViewer: Boolean = false,
    oneLineMode: Boolean = true
) :
    NonOpaquePanel(), EditorTextComponent, DocumentListener, DataProvider,
    TextAccessor, FocusListener, MouseListener {
    private var myDocument: Document? = null
    val project: Project?
    private var fileType: FileType? = null
    private var myEditor: EditorEx? = null
    private var myWholeTextSelected = false
    private val myDocumentListeners = ContainerUtil.createLockFreeCopyOnWriteList<DocumentListener>()
    private val myFocusListeners = ContainerUtil.createLockFreeCopyOnWriteList<FocusListener>()
    private val myMouseListeners = ContainerUtil.createLockFreeCopyOnWriteList<MouseListener>()
    private var myIsListenerInstalled = false
    private var isViewer = false
    private var myIsSupplementary = false
    private var myInheritSwingFont = true
    private var myEnforcedBgColor: Color? = null

    private var isOneLineMode = false
    private var myEnsureWillComputePreferredSize = false
    private var myPassivePreferredSize: Dimension? = null
    private var myHintText: CharSequence? = null
    private var myIsRendererWithSelection = false
    private var myRendererBg: Color? = null
    private var myRendererFg: Color? = null
    private var myPreferredWidth = -1
    private var myCaretPosition = -1
    private val mySettingsProviders: MutableList<EditorSettingsProvider> = ArrayList()
    private var myDisposable: Disposable? = null

    @JvmOverloads
    constructor(text: String = "") : this(EditorFactory.getInstance().createDocument(text), null, FileTypes.PLAIN_TEXT)
    constructor(text: String, project: Project?, fileType: FileType?) : this(
        EditorFactory.getInstance().createDocument(text), project, fileType, false, true
    )

    constructor(project: Project?, fileType: FileType?) : this(null, project, fileType, false, false)

    init {
        isOneLineMode = oneLineMode
        this.isViewer = isViewer
        setDocument(document)
        this.project = project
        this.fileType = fileType
        layout = BorderLayout()
        enableEvents(AWTEvent.KEY_EVENT_MASK)
        isFocusable = true
        super.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                requestFocus()
            }
        })
        isFocusTraversalPolicyProvider = true
        focusTraversalPolicy = Jdk7DelegatingToRootTraversalPolicy()
        font = UIManager.getFont("TextField.font")
    }

    fun setSupplementary(supplementary: Boolean) {
        myIsSupplementary = supplementary
        if (myEditor != null) {
            myEditor!!.putUserData(SUPPLEMENTARY_KEY, supplementary)
        }
    }

    fun setFontInheritedFromLAF(b: Boolean) {
        myInheritSwingFont = b
        setDocument(myDocument) // reinit editor.
    }

    override fun getText(): String {
        return myDocument!!.text
    }

    override fun setBackground(bg: Color) {
        super.setBackground(bg)
        myEnforcedBgColor = bg
        if (myEditor != null) {
            myEditor!!.backgroundColor = bg
        }
    }

    override fun getComponent(): JComponent {
        return this
    }

    override fun addDocumentListener(listener: DocumentListener) {
        myDocumentListeners.add(listener)
        installDocumentListener()
    }

    override fun removeDocumentListener(listener: DocumentListener) {
        myDocumentListeners.remove(listener)
        uninstallDocumentListener(false)
    }

    override fun beforeDocumentChange(event: DocumentEvent) {
        for (documentListener in myDocumentListeners) {
            documentListener.beforeDocumentChange(event)
        }
    }

    override fun documentChanged(event: DocumentEvent) {
        for (documentListener in myDocumentListeners) {
            documentListener.documentChanged(event)
        }
    }

    override fun getDocument(): Document {
        if (myDocument == null) {
            myDocument = createDocument()
        }
        return myDocument!!
    }

    private fun setDocument(document: Document?) {
        if (myDocument != null) {
            uninstallDocumentListener(true)
        }
        myDocument = document
        installDocumentListener()
        if (myEditor != null) {
            val isFocused = isFocusOwner
            val newEditor = createEditor()
            releaseEditor(myEditor)
            myEditor = newEditor
            add(myEditor!!.component, BorderLayout.CENTER)
            validate()
            if (isFocused) {
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
                    IdeFocusManager.getGlobalInstance().requestFocus(newEditor.contentComponent, true)
                }
            }
        }
    }

    private fun installDocumentListener() {
        if (myDocument != null && myDocumentListeners.isNotEmpty() && !myIsListenerInstalled) {
            myIsListenerInstalled = true
            myDocument!!.addDocumentListener(this)
        }
    }

    private fun uninstallDocumentListener(force: Boolean) {
        if (myDocument != null && myIsListenerInstalled && (force || myDocumentListeners.isEmpty())) {
            myIsListenerInstalled = false
            myDocument!!.removeDocumentListener(this)
        }
    }

    override fun setText(text: String?) {
        CommandProcessor.getInstance().executeCommand(
            project,
            {
                ApplicationManager.getApplication().runWriteAction {
                    myDocument!!.replaceString(
                        0,
                        myDocument!!.textLength,
                        StringUtil.notNullize(text)
                    )
                    if (myEditor != null) {
                        val caretModel = myEditor!!.caretModel
                        if (caretModel.offset >= myDocument!!.textLength) {
                            caretModel.moveToOffset(myDocument!!.textLength)
                        }
                    }
                }
            }, null, null, UndoConfirmationPolicy.DEFAULT, document
        )
    }

    /**
     * Allows to define [editor&#39;s placeholder][EditorEx.setPlaceholder]. The trick here is that the editor
     * is instantiated lazily by the editor text field and provided placeholder text is applied to the editor during its
     * actual construction then.
     *
     * @param text [editor&#39;s placeholder][EditorEx.setPlaceholder] text to use
     */
    fun setPlaceholder(text: CharSequence?) {
        myHintText = text
        if (myEditor != null) {
            myEditor!!.setPlaceholder(text)
        }
    }

    fun selectAll() {
        if (myEditor != null) {
            doSelectAll(myEditor!!)
        } else {
            myWholeTextSelected = true
        }
    }

    fun removeSelection() {
        if (myEditor != null) {
            myEditor!!.selectionModel.removeSelection()
        } else {
            myWholeTextSelected = false
        }
    }

    /**
     * @see javax.swing.text.JTextComponent.setCaretPosition
     */
    fun setCaretPosition(position: Int) {
        val document = document
        require(!(position > document.textLength || position < 0)) { "bad position: $position" }
        if (myEditor != null) {
            myEditor!!.caretModel.moveToOffset(position)
        } else {
            myCaretPosition = position
        }
    }

    val caretModel: CaretModel
        get() = myEditor!!.caretModel

    override fun isFocusOwner(): Boolean {
        return if (myEditor != null) {
            IJSwingUtilities.hasFocus(myEditor!!.contentComponent)
        } else super.isFocusOwner()
    }

    override fun addNotify() {
        myDisposable = Disposer.newDisposable("ETF dispose")
        Disposer.register(myDisposable!!) { releaseEditorLater() }
        if (project != null) {
            val listener: ProjectManagerListener = object : ProjectManagerListener {
                override fun projectClosing(project: Project) {
                    releaseEditor(myEditor)
                    myEditor = null
                }
            }
            ProjectManager.getInstance().addProjectManagerListener(project, listener)
            Disposer.register(
                myDisposable!!
            ) {
                ProjectManager.getInstance().removeProjectManagerListener(project, listener)
            }
        }
        if (myEditor != null) {
            releaseEditorLater()
        }
        val isFocused = isFocusOwner
        initEditor()
        super.addNotify()
        revalidate()
        if (isFocused) {
            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown { requestFocus() }
        }
    }

    private fun initEditor() {
        myEditor = createEditor()
        myEditor!!.contentComponent.isEnabled = isEnabled
        if (myCaretPosition >= 0) {
            myEditor!!.caretModel.moveToOffset(myCaretPosition)
            myEditor!!.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
        }
        val tooltip = toolTipText
        if (StringUtil.isNotEmpty(tooltip)) {
            myEditor!!.contentComponent.toolTipText = tooltip
        }
        add(myEditor!!.component, BorderLayout.CENTER)
    }

    override fun removeNotify() {
        super.removeNotify()
        if (myDisposable != null) {
            Disposer.dispose(myDisposable!!)
        }
    }

    private fun releaseEditor(editor: Editor?) {
        if (editor == null) return
        if (project != null && !project.isDisposed && isViewer) {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, true)
            }
        }
        remove(editor.component)
        editor.contentComponent.removeFocusListener(this)
        editor.contentComponent.removeMouseListener(this)
        if (!editor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    private fun releaseEditorLater() {
        val editor = myEditor
        ApplicationManager.getApplication()
            .invokeLater({ releaseEditor(editor) }, ModalityState.stateForComponent(this))
        myEditor = null
    }

    override fun setFont(font: Font) {
        super.setFont(font)
        if (myEditor != null) {
            setupEditorFont(myEditor!!)
        }
    }

    private fun initOneLineMode(editor: EditorEx) {
        val isOneLineMode = isOneLineMode

        // set mode in editor
        editor.isOneLineMode = isOneLineMode
        val customGlobalScheme = editor.colorsScheme
        editor.colorsScheme = editor.createBoundColorSchemeDelegate(if (isOneLineMode) customGlobalScheme else null)
        editor.settings.isCaretRowShown = false
    }

    private fun createDocument(): Document? {
        val factory = PsiFileFactory.getInstance(project)
        val stamp = LocalTimeCounter.currentTime()
        val psiFile = factory.createFileFromText(
            "Dummy." + fileType!!.defaultExtension,
            fileType!!, "", stamp, true, false
        )
        return PsiDocumentManager.getInstance(project!!).getDocument(psiFile)
    }

    private fun createEditor(): EditorEx {
        val document = document
        val factory = EditorFactory.getInstance()
        val editor = (if (isViewer) factory.createViewer(document, project) else factory.createEditor(
            document,
            project
        )) as EditorEx
        setupTextFieldEditor(editor)
        editor.setCaretEnabled(!isViewer)
        if (project != null) {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, !isViewer)
            }
        }
        if (project != null) {
            val highlighterFactory = EditorHighlighterFactory.getInstance()
            val virtualFile = if (myDocument == null) null else FileDocumentManager.getInstance().getFile(
                myDocument!!
            )
            val highlighter = if (virtualFile != null) highlighterFactory.createEditorHighlighter(
                project, virtualFile
            ) else if (fileType != null) highlighterFactory.createEditorHighlighter(
                project,
                fileType!!
            ) else null
            if (highlighter != null) editor.highlighter = highlighter
        }
        editor.settings.isCaretRowShown = false
        editor.isOneLineMode = isOneLineMode
        editor.caretModel.moveToOffset(document.textLength)
        if (!shouldHaveBorder()) {
            editor.setBorder(null)
        }
        if (isViewer) {
            editor.selectionModel.removeSelection()
        } else if (myWholeTextSelected) {
            doSelectAll(editor)
            myWholeTextSelected = false
        }
        editor.putUserData(SUPPLEMENTARY_KEY, myIsSupplementary)
        editor.contentComponent.isFocusCycleRoot = false
        editor.contentComponent.addFocusListener(this)
        editor.contentComponent.addMouseListener(this)
        editor.setPlaceholder(myHintText)
        initOneLineMode(editor)
        if (myIsRendererWithSelection) {
            (editor as EditorImpl).isPaintSelection = true
            editor.getColorsScheme().setColor(EditorColors.SELECTION_BACKGROUND_COLOR, myRendererBg)
            editor.getColorsScheme().setColor(EditorColors.SELECTION_FOREGROUND_COLOR, myRendererFg)
            editor.selectionModel.setSelection(0, document.textLength)
            editor.setBackgroundColor(myRendererBg)
        }
        for (provider in mySettingsProviders) {
            provider.customizeSettings(editor)
        }
        return editor
    }

    private fun setupEditorFont(editor: EditorEx) {
        if (myInheritSwingFont) {
            (editor as EditorImpl).setUseEditorAntialiasing(false)
            editor.getColorsScheme().editorFontName = font.fontName
            editor.getColorsScheme().editorFontSize = font.size
            return
        }
        val settings = UISettings.getInstance()
        if (settings.presentationMode) editor.setFontSize(settings.presentationModeFontSize)
    }

    private fun shouldHaveBorder(): Boolean {
        return true
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            super.setEnabled(enabled)
            isFocusTraversalPolicyProvider = enabled
            setViewerEnabled(enabled)
            val editor = myEditor
            if (editor != null) {
                releaseEditor(editor)
                initEditor()
                revalidate()
            }
        }
    }

    private fun setViewerEnabled(enabled: Boolean) {
        isViewer = !enabled
    }

    override fun getBackground(): Color {
        return getBackgroundColor(isEnabled, EditorColorsUtil.getGlobalOrDefaultColorScheme())
    }

    private fun getBackgroundColor(enabled: Boolean, colorsScheme: EditorColorsScheme): Color {
        if (myEnforcedBgColor != null) return myEnforcedBgColor!!
        if (ComponentUtil.getParentOfType(
                CellRendererPane::class.java as Class<out CellRendererPane?>,
                this as Component
            ) != null && (StartupUiUtil
                .isUnderDarcula() || UIUtil.isUnderIntelliJLaF())
        ) {
            return parent.background
        }
        if (StartupUiUtil.isUnderDarcula()) return UIUtil.getTextFieldBackground()
        return if (enabled) colorsScheme.defaultBackground else UIUtil.getInactiveTextFieldBackgroundColor()
    }

    override fun addImpl(comp: Component, constraints: Any, index: Int) {
        if (myEditor == null || comp !== myEditor!!.component) {
            assert(false) { "You are not allowed to add anything to EditorTextField" }
        }
        super.addImpl(comp, constraints, index)
    }

    override fun getPreferredSize(): Dimension {
        if (isPreferredSizeSet) {
            return super.getPreferredSize()
        }
        var toReleaseEditor = false
        if (myEditor == null && myEnsureWillComputePreferredSize) {
            myEnsureWillComputePreferredSize = false
            initEditor()
            toReleaseEditor = true
        }
        var size: Dimension = JBUI.size(100, 10)
        if (myEditor != null) {
            val preferredSize = myEditor!!.component.preferredSize
            if (myPreferredWidth != -1) {
                preferredSize.width = myPreferredWidth
            }
            JBInsets.addTo(preferredSize, insets)
            size = preferredSize
        } else if (myPassivePreferredSize != null) {
            size = myPassivePreferredSize as Dimension
        }
        if (toReleaseEditor) {
            releaseEditor(myEditor)
            myEditor = null
            myPassivePreferredSize = size
        }
        return size
    }

    override fun getMinimumSize(): Dimension {
        if (isMinimumSizeSet) {
            return super.getMinimumSize()
        }
        val size: Dimension = JBUI.size(1, 10)
        if (myEditor != null) {
            size.height = myEditor!!.lineHeight
            if (UIUtil.isUnderDefaultMacTheme() || StartupUiUtil.isUnderDarcula() || UIUtil.isUnderIntelliJLaF()) {
                size.height = size.height.coerceAtLeast(scale(16))
            }
            JBInsets.addTo(size, insets)
            JBInsets.addTo(size, myEditor!!.insets)
        }
        return size
    }

    fun setPreferredWidth(preferredWidth: Int) {
        myPreferredWidth = preferredWidth
    }



    override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
        return if (e.isConsumed || myEditor != null && !myEditor!!.processKeyTyped(e)) {
            super.processKeyBinding(ks, e, condition, pressed)
        } else true
    }

    override fun requestFocus() {
        if (myEditor != null) {
            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
                if (myEditor != null) IdeFocusManager.getGlobalInstance().requestFocus(
                    myEditor!!.contentComponent, true
                )
            }
            myEditor!!.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
        }
    }

    override fun requestFocusInWindow(): Boolean {
        return if (myEditor != null) {
            val b = myEditor!!.contentComponent.requestFocusInWindow()
            myEditor!!.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
            b
        } else {
            super.requestFocusInWindow()
        }
    }

    val editor: Editor?
        /**
         * @return null if the editor is not initialized (e.g. if the field is not added to a container)
         * @see .createEditor
         * @see .addNotify
         */
        get() = myEditor
    val focusTarget: JComponent
        get() = if (myEditor == null) this else myEditor!!.contentComponent

    @Synchronized
    override fun addFocusListener(l: FocusListener) {
        myFocusListeners.add(l)
    }

    @Synchronized
    override fun removeFocusListener(l: FocusListener) {
        myFocusListeners.remove(l)
    }

    override fun focusGained(e: FocusEvent) {
        for (listener in myFocusListeners) {
            listener.focusGained(e)
        }
    }

    override fun focusLost(e: FocusEvent) {
        for (listener in myFocusListeners) {
            listener.focusLost(e)
        }
    }

    override fun addMouseListener(l: MouseListener) {
        myMouseListeners.add(l)
    }

    override fun removeMouseListener(l: MouseListener) {
        myMouseListeners.remove(l)
    }

    override fun mouseClicked(e: MouseEvent) {
        for (listener in myMouseListeners) {
            listener.mouseClicked(e)
        }
    }

    override fun mousePressed(e: MouseEvent) {
        for (listener in myMouseListeners) {
            listener.mousePressed(e)
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        for (listener in myMouseListeners) {
            listener.mouseReleased(e)
        }
    }

    override fun mouseEntered(e: MouseEvent) {
        for (listener in myMouseListeners) {
            listener.mouseEntered(e)
        }
    }

    override fun mouseExited(e: MouseEvent) {
        for (listener in myMouseListeners) {
            listener.mouseExited(e)
        }
    }

    override fun getData(dataId: String): Any? {
        if (myEditor != null && myEditor!!.isRendererMode) {
            return if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) {
                myEditor!!.copyProvider
            } else null
        }
        return if (CommonDataKeys.EDITOR.`is`(dataId)) {
            myEditor
        } else null
    }

    fun setFileType(fileType: FileType) {
        setNewDocumentAndFileType(fileType, document)
    }

    fun setNewDocumentAndFileType(fileType: FileType, document: Document?) {
        this.fileType = fileType
        setDocument(document)
    }

    fun ensureWillComputePreferredSize() {
        myEnsureWillComputePreferredSize = true
    }

    fun setAsRendererWithSelection(backgroundColor: Color?, foregroundColor: Color?) {
        myIsRendererWithSelection = true
        myRendererBg = backgroundColor
        myRendererFg = foregroundColor
    }

    fun addSettingsProvider(provider: EditorSettingsProvider) {
        mySettingsProviders.add(provider)
    }

    fun removeSettingsProvider(provider: EditorSettingsProvider): Boolean {
        return mySettingsProviders.remove(provider)
    }

    private class Jdk7DelegatingToRootTraversalPolicy : AbstractDelegatingToRootTraversalPolicy() {
        private var invokedFromBeforeOrAfter = false
        override fun getFirstComponent(aContainer: Container): Container? {
            return getDefaultComponent(aContainer)
        }

        override fun getLastComponent(aContainer: Container): Container? {
            return getDefaultComponent(aContainer)
        }

        override fun getComponentAfter(aContainer: Container, aComponent: Component): Component? {
            invokedFromBeforeOrAfter = true
            val after: Component
            after = try {
                super.getComponentAfter(aContainer, aComponent)
            } finally {
                invokedFromBeforeOrAfter = false
            }
            return if (after !== aComponent) after else null // escape our container
        }

        override fun getComponentBefore(aContainer: Container, aComponent: Component): Component? {
            val before = super.getComponentBefore(aContainer, aComponent)
            return if (before !== aComponent) before else null // escape our container
        }

        override fun getDefaultComponent(aContainer: Container): Container? {
            if (invokedFromBeforeOrAfter) return null // escape our container
            val editor = if (aContainer is EditorTextPanel) aContainer.editor else null
            return editor?.contentComponent ?: aContainer
        }
    }

    companion object {
        val SUPPLEMENTARY_KEY = Key.create<Boolean>("Supplementary")
        private fun doSelectAll(editor: Editor) {
            editor.caretModel.removeSecondaryCarets()
            editor.caretModel.primaryCaret.setSelection(0, editor.document.textLength, false)
        }

        fun setupTextFieldEditor(editor: EditorEx) {
            val settings = editor.settings
            settings.additionalLinesCount = 0
            settings.additionalColumnsCount = 1
            settings.isRightMarginShown = false
            settings.setRightMargin(-1)
            settings.isFoldingOutlineShown = true
            settings.isLineNumbersShown = true
            settings.isLineMarkerAreaShown = true
            settings.isIndentGuidesShown = true
            settings.isVirtualSpace = false
            settings.isWheelFontChangeEnabled = false
            settings.isAdditionalPageAtBottom = false
            editor.setHorizontalScrollbarVisible(true)
            editor.setVerticalScrollbarVisible(true)
            settings.lineCursorWidth = 1
        }
    }
}