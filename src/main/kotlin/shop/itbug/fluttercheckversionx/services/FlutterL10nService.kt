package shop.itbug.fluttercheckversionx.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.intellij.ui.JBColor
import com.intellij.util.messages.Topic
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartArgumentsImpl
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartNamedArgumentImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.tools.JsonPsiFactory
import shop.itbug.fluttercheckversionx.tools.MyJsonPsiUtil
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.string
import shop.itbug.fluttercheckversionx.window.l10n.FlutterL10nKeyEditPanel
import shop.itbug.fluttercheckversionx.window.l10n.MyL10nKeysTree
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

data class DartString(
    val element: SmartPsiElementPointer<DartStringLiteralExpressionImpl>,
    val file: VirtualFile,
    val psiFile: DartFile,
    val text: String
) {
    override fun toString(): String {
        return text
    }
}

fun List<DartString>.group(): Map<VirtualFile, List<DartString>> {
    return groupBy { it.file }
}

data class L10nKeyItem(
    val key: String, val value: String, val property: SmartPsiElementPointer<JsonProperty>, val file: ArbFile,

    var range: TextRange? = null
) : UserDataHolderBase() {
    override fun toString(): String {
        return "$key:$value"
    }
}

data class ArbFile(
    val file: VirtualFile, var psiFile: PsiFile, val project: Project,
    var keyItems: List<L10nKeyItem> = emptyList(),
    val originPsiFile: PsiFile,
) : UserDataHolderBase() {
    override fun toString(): String {
        return file.name
    }
}

@Service(Service.Level.PROJECT)
class FlutterL10nService(val project: Project) : Disposable {


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val config = PluginConfig.getState(project)
    private var writeJob: Job? = null
    private var edtJob: Job? = null
    var dartStringList: List<DartString> = emptyList()

    ///扫描项目中的所有字符串
    private var scanStringsJob: Job? = null


    fun coroutineScope() = scope
    private suspend fun keys(): List<L10nKeyItem> =
        arbFiles.map { scope.async { it.readAllKeys() } }.awaitAll().toList().flatten()

    var arbFiles = listOf<ArbFile>() //所有 arb filesp


    //开始扫描项目中的所有字符串 psi element
    fun startScanStringElements() {
        scanStringsJob?.cancel()
        scanStringsJob = scope.launch(Dispatchers.IO) {
            //获取所有项目文件
            println("开始扫描项目中所有字符串")
            val allFiles = readAllExitStringElementFiles()
            val eleList = allFiles.map { async { readAllStringElements(it) } }.awaitAll().flatten()
            dartStringList = eleList
            project.messageBus.syncPublisher(OnDartStringScanCompleted)
                .onDartStringScanCompleted(project, dartStringList, dartStringList.group())
        }
    }


    ///获取所有 dart 字符串节点
    private suspend fun readAllStringElements(file: VirtualFile): List<DartString> {
        val psi = readAction {
            try {
                PsiManager.getInstance(project).findFile(file)
            } catch (_: Exception) {
                null
            }
        } ?: return emptyList()
        val list =
            readAction { PsiTreeUtil.findChildrenOfType(psi, DartStringLiteralExpressionImpl::class.java) }.toList()
        return list.mapNotNull {
            val parent = readAction { it.parent }
            if (parent is DartArgumentsImpl || parent is DartNamedArgumentImpl) {
                val text = it.string ?: return@mapNotNull null
                val classContain =
                    readAction { PsiTreeUtil.findFirstParent(it) { e -> e is DartClassDefinitionImpl } } as? DartClassDefinitionImpl
                if (classContain != null) {
                    val isFreezed = readAction { classContain.myManagerFun().isFreezed3Class() }
                    if (isFreezed) {
                        return@mapNotNull null
                    }
                }
                val point = readAction { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
                return@mapNotNull DartString(
                    element = point,
                    file = file,
                    psiFile = psi as DartFile,
                    text = text,
                )
            }
            return@mapNotNull null
        }
    }

    ///获取索引中所有带有字符串的文件
    private suspend fun readAllExitStringElementFiles(): List<VirtualFile> {
        val flutterLibVirtualFile = readAction { MyFileUtil.getFlutterLibVirtualFile(project) } ?: return emptyList()
        val libScope = GlobalSearchScopes.directoryScope(project, flutterLibVirtualFile, true)
        val files = readAction {
            FileTypeIndex.getFiles(DartFileType.INSTANCE, libScope)
        }
        return files.filterNotNull().filter {
            if (it.name.endsWith(".g.dart")) return@filter false
            if (it.name.endsWith(".freezed.dart")) return@filter false
            return@filter true
        }
    }

    //检测 l18n所有的 key
    suspend fun checkAllKeys() {
        val folder = config.l10nFolder ?: return
        val file = readAction { LocalFileSystem.getInstance().findFileByPath(folder) } ?: return
        val directory = withContext(Dispatchers.IO) {
            ApplicationManager.getApplication().executeOnPooledThread<PsiDirectory?> {
                return@executeOnPooledThread runReadAction { PsiManager.getInstance(project).findDirectory(file) }
            }.get()
        }
        if (directory != null) {
            val files = readAction { directory.files }
            suspend fun handleArbFile(psiFile: PsiFile): ArbFile? {
                val vf = readAction { psiFile.virtualFile }
                if (vf.extension == "arb") {
                    val arbFile = ArbFile(vf, psiFile, project, originPsiFile = psiFile)
                    arbFile.readAllKeys()
                    return arbFile
                }
                return null
            }

            arbFiles = files.mapNotNull { file -> scope.async { handleArbFile(file) } }.awaitAll().filterNotNull()
        }
        pushTopic()
    }

    fun handleKeys(call: (keyList: List<String>) -> Unit) {
        scope.launch {
            val keys = keys()
            call(getAllKeyTexts(keys))
        }
    }

    ///获取所有的 key (去重过)
    fun getAllKeyTexts(items: List<L10nKeyItem>): List<String> {
        return items.map { it.key }.distinct()
    }


    fun runWriteThread(run: suspend () -> Unit) {
        writeJob?.cancel(CancellationException("写入关闭"))
        writeJob = scope.launch(Dispatchers.IO) {
            run()
        }
    }

    fun runEdtThread(run: suspend () -> Unit) {
        edtJob?.cancel(CancellationException("edt任务关闭"))
        edtJob = scope.launch(Dispatchers.EDT) {
            run.invoke()
        }
    }

    // 执行 flutter gen-l10n命令
    fun runFlutterGenL10nCommand() {
        val commandLine = GeneralCommandLine("flutter", "gen-l10n")
        commandLine.workDirectory = project.guessProjectDir()?.toNioPath()?.toFile()
        scope.launch {
            try {
                ExecUtil.execAndReadLine(commandLine)
            } catch (_: Exception) {
            }
        }
    }

    //插入新的 key键
    fun insetNewKey(newKey: String) {
        scope.launch {
            arbFiles.map {
                scope.async(Dispatchers.IO) {
                    it.insetNewKey(newKey)
                }
            }.awaitAll()
            //刷新ui中的 key
            pushTopic()
        }
    }

    fun refreshKeys() {
        pushTopic()
    }

    private fun pushTopic() {
        scope.launch {
            val items = keys()
            project.messageBus.syncPublisher(ListenKeysChanged).onKeysChanged(items, getAllKeyTexts(items), project)
        }
    }

    //更新了 l10n配置
    fun configEndTheL10nFolder() {
        val folder = PluginConfig.getState(project).l10nFolder ?: return
        if (folder.isNotBlank()) {
            arbFiles = emptyList()
            scope.launch {
                checkAllKeys()
            }
        }
    }


    override fun dispose() {
        scope.cancel()
        writeJob?.cancel()
    }

    interface OnL10nKeysChangedListener {
        fun onKeysChanged(items: List<L10nKeyItem>, keysString: List<String>, project: Project)
    }

    interface OnArbFileChangedListener {
        fun onArbFileChanged(arbFile: ArbFile)
    }

    interface OnTreeKeyChanged {
        fun onTreeKeyChanged(project: Project, key: String, tree: MyL10nKeysTree, panels: List<FlutterL10nKeyEditPanel>)
    }

    interface OnDartStringScanCompletedListener {
        fun onDartStringScanCompleted(
            project: Project, list: List<DartString>, group: Map<VirtualFile, List<DartString>>
        )
    }

    companion object {
        fun getInstance(project: Project) = project.service<FlutterL10nService>()
        val ListenKeysChanged =
            Topic<OnL10nKeysChangedListener>.create("ListenKeysChanged", OnL10nKeysChangedListener::class.java)
        val ArbFileChanged =
            Topic<OnArbFileChangedListener>.create("ArbFileChanged", OnArbFileChangedListener::class.java)

        //key选择发生变更
        val TreeKeyChanged = Topic<OnTreeKeyChanged>.create("L10nKeysTreeShowOnChanged", OnTreeKeyChanged::class.java)

        //dart字符串扫描完毕事件
        val OnDartStringScanCompleted = Topic<OnDartStringScanCompletedListener>(
            "OnDartStringScanCompleted", OnDartStringScanCompletedListener::class.java
        )

    }
}

suspend fun ArbFile.readAllKeys(): List<L10nKeyItem> {
    val props = allJsonProperties()
    val items = readAction {
        props.map { property ->
            return@map L10nKeyItem(
                key = property.nameString(),
                value = property.valueString(),
                file = this,
                property = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(property),
                range = property.textRange
            )
        }.toList()
    }
    keyItems = items
    return items
}


fun ArbFile.allJsonProperties(): List<JsonProperty> {
    return runReadAction {
        val jsonFile = toJsonFile()
        val jsonObject = jsonFile.topLevelValue as JsonObject
        val pros = jsonObject.propertyList
        pros
    }
}

fun ArbFile.toJsonFile(): JsonFile {
    val text = ApplicationManager.getApplication().executeOnPooledThread<String> { file.readText() }.get()
    psiFile = PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE, text) as JsonFile
    return psiFile as JsonFile
}


//读取 key对应的值
fun ArbFile.readValue(key: String): String {
    val props = allJsonProperties()
    return props.find { it.nameString() == key }?.valueString() ?: ""
}

//重新写入 key value
suspend fun ArbFile.reWriteKeyValue(key: String, newValue: String) {
    val item = readAllKeys().find { it.key == key }
    if (item == null) {
        //插入新key
        insetNewKey(key, newValue)
    }
    if (item != null) {
        val jsonProp = readAction { item.property.element } ?: return
        val valueEle = readAction { jsonProp.value } as? JsonStringLiteral ?: return
        val newEle = readAction { JsonPsiFactory.createJsonStringLiteral(project, newValue) }
        WriteCommandAction.runWriteCommandAction(project) {
            valueEle.replace(newEle)
            reWriteFile()
        }
    }
}


suspend fun ArbFile.hasKey(key: String): Boolean {
    return readAllKeys().any { it.key == key }
}

//插入新的 key
suspend fun ArbFile.insetNewKey(newKey: String, value: String = "") {
    //判断是不是已经存在这个 key
    if (hasKey(newKey)) {
        println("已经存在这个$newKey,忽略")
        return
    }
    val topObj = readAction { (psiFile as JsonFile).topLevelValue as? JsonObject } ?: return
    val newEle = readAction { JsonPsiFactory.createNewJsonProp(project, newKey, value) }
    WriteCommandAction.runWriteCommandAction(project) {
        JsonPsiUtil.addProperty(topObj, newEle, false)
        reWriteFile()
    }
}


private fun ArbFile.reWriteFile(fireEvent: Boolean = true) {
    CodeStyleManager.getInstance(project).reformat(psiFile)
    val document = runReadAction { psiFile.fileDocument }
    FileDocumentManager.getInstance().saveDocument(document)
    VfsUtil.saveText(file, psiFile.text)
    //重新替换 psiFile
    toJsonFile()
    if (fireEvent) {
        project.messageBus.syncPublisher(FlutterL10nService.ArbFileChanged).onArbFileChanged(this)
    }

}


fun ArbFile.moveToOffset(key: String, editor: Editor) {
    FlutterL10nService.getInstance(project).runEdtThread {
        val keys = readAllKeys()
        val find = keys.find { it.key == key } ?: return@runEdtThread
        val ele = readAction { find.property.element } ?: return@runEdtThread
        val markup = editor.markupModel
        val startOffset = readAction { ele.startOffset }
        val endOffset = readAction { ele.endOffset }
        editor.caretModel.moveToOffset(startOffset)
        editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
        val highlighter = markup.addRangeHighlighter(
            startOffset, endOffset, HighlighterLayer.CARET_ROW + 1, TextAttributes().apply {
                backgroundColor = JBColor.blue
            }, HighlighterTargetArea.EXACT_RANGE
        )
        Timer().scheduleAtFixedRate(0, 1000) {
            markup.removeHighlighter(highlighter)
            this.cancel()
        }
    }
}

/**
 * 删除$[key]
 */
suspend fun ArbFile.removeKey(key: String) {
    val readAllKeys = readAllKeys()
    val find = readAllKeys.find { it.key == key }
    if (find != null) {
        val ele = readAction { find.property.element }
        if (ele != null) {
            WriteCommandAction.runWriteCommandAction(project) {
                MyJsonPsiUtil.removeJsonProperty(ele)
                reWriteFile(false)
            }

        }
    }
}

/**
 * 重命名 key
 */
suspend fun ArbFile.renameKey(key: String, newKey: String) {
    val readAllKeys = readAllKeys()
    val find = readAllKeys.find { it.key == key }
    if (find != null) {
        val ele = readAction { find.property.element }
        if (ele != null) {
            ///重新命名
            val newKeyEle = readAction { JsonPsiFactory.createJsonStringLiteral(project, newKey) }
            val nameEle = ele.nameElement as? JsonStringLiteral
            if (nameEle != null) {
                WriteCommandAction.runWriteCommandAction(project) {
                    nameEle.replace(newKeyEle)
                    reWriteFile(false)
                }
            }
        }
    }
}

private fun JsonProperty.nameString(): String {
    return name.replace("\"", "")
}


private fun JsonProperty.valueString(): String {
    return (value as? JsonStringLiteral)?.value?.replace("\"", "") ?: ""
}