package shop.itbug.fluttercheckversionx.services

import com.google.dart.server.AnalysisServerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartNamedArgument
import com.jetbrains.lang.dart.psi.DartReferenceExpression
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.dartlang.analysis.server.protocol.*
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import kotlin.coroutines.CoroutineContext

data class DotRemoveElement(
    val element: SmartPsiElementPointer<PsiElement>,
    val removeOffsetStart: Int,
    val removeOffsetEnd: Int,
    val enumType: String,
    val fullText: String
) {
    override fun toString(): String {
        return "{枚举类型:$enumType 从$removeOffsetStart,删除到$removeOffsetEnd,完整文本:$fullText}"
    }
}

data class ScanProgress(
    val totalArguments: Int,
    val processedCount: Int,
    val foundEnums: Int
)

@Service(Service.Level.PROJECT)
class DotMigrateService(val project: Project) : Disposable, CoroutineScope, AnalysisServerListener {
    private val job = SupervisorJob()
    private val smartPointerManager = SmartPointerManager.getInstance(project)
    private val logger = thisLogger()
    private val dartService = DartAnalysisServerService.getInstance(project)
    private var cachedElements: MutableList<DotRemoveElement> = mutableListOf()

    init {
        dartService.addAnalysisServerListener(this)
    }

    fun getScannedElements(): List<DotRemoveElement> {
        return cachedElements.toList()
    }

    fun removeElement(element: DotRemoveElement) {
        cachedElements.remove(element)
    }

    suspend fun refreshScan(
        onProgress: (ScanProgress) -> Unit = {},
        onElementFound: (DotRemoveElement) -> Unit = {}
    ): List<DotRemoveElement> {
        logger.info("开始刷新扫描所有枚举类型")
        cachedElements.clear()
        cachedElements.addAll(startScanProjectFiles(onProgress, onElementFound))
        logger.info("刷新扫描完成: ${cachedElements.size}个")
        return cachedElements.toList()
    }

    fun startCheck() {
        DumbService.getInstance(project).runWhenSmart {
            launch {
                logger.info("开始扫描所有枚举类型")
                cachedElements.clear()
                cachedElements.addAll(startScanProjectFiles())
                logger.info("扫描到所有枚举的赋值: ${cachedElements.size}个, 详情: $cachedElements")
            }
        }

    }

    private suspend fun startScanProjectFiles(
        onProgress: (ScanProgress) -> Unit = {},
        onElementFound: (DotRemoveElement) -> Unit = {}
    ): List<DotRemoveElement> {
        // 1. 在一个 readAction 中收集所有需要分析的元素
        val allNamedArguments = readAction {
            val flutterLibVirtualFile = MyFileUtil.getFlutterLibVirtualFile(project) ?: return@readAction emptyList()
            val libScope = GlobalSearchScopes.directoryScope(project, flutterLibVirtualFile, true)
            val allDartFiles = FileTypeIndex.getFiles(DartFileType.INSTANCE, libScope)
            allDartFiles.flatMap { virtualFile ->
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@flatMap emptyList()
                PsiTreeUtil.collectElementsOfType(psiFile, DartNamedArgument::class.java)
            }
        }
        logger.info("所有参数节点:${allNamedArguments.size}")

        val totalCount = allNamedArguments.size
        var processedCount = 0
        var foundCount = 0
        val results = mutableListOf<DotRemoveElement>()

        val semaphore = Semaphore(permits = 1)
        logger.info("开始循环遍历分析枚举赋值")

        withContext(Dispatchers.IO) {
            allNamedArguments.map { argument ->
                async {
                    semaphore.withPermit {
                        val result = analyzeArgument(argument)
                        synchronized(results) {
                            processedCount++
                            if (result != null) {
                                foundCount++
                                results.add(result)
                                onElementFound(result)
                            }
                            onProgress(ScanProgress(totalCount, processedCount, foundCount))
                        }
                        delay(10)
                        result
                    }
                }
            }.awaitAll()
        }

        logger.info("分析完毕:${results.size}")
        return results
    }

    /**
     * 使用静态PSI解析来分析一个命名参数的值是否为枚举。
     * 这是一个纯CPU操作，速度非常快，且不需要IPC。
     */
    private suspend fun analyzeArgument(element: DartNamedArgument): DotRemoveElement? {
        return readAction {
            val file = element.containingFile.virtualFile ?: return@readAction  null
            if (!element.isValid) return@readAction null
            val argNameEle = element.parameterReferenceExpression
            val argNameInfo = getElementKind(argNameEle)
            logger.info("arg:${argNameEle.text} : info: $argNameInfo")
            val argType = argNameInfo?.type
            if(argType == "dynamic" || argType == "Object" || argType == "Object?") {
                logger.info("参数:${argNameEle.text}被忽略")
                return@readAction null
            }

            val expression = element.expression
            val firstExp = expression.firstChild as? DartReferenceExpression ?: return@readAction null

            val childrenRefSize = PsiTreeUtil.countChildrenOfType(firstExp, DartReferenceExpression::class.java)
            if(childrenRefSize>=2){
                logger.info("${element.text}被忽略,存在多个 dart 引用")
                return@readAction null
            }

            if (expression.lastChild !is DartReferenceExpression) return@readAction null
            val eleKind = getElementKind(firstExp) ?: return@readAction null
            if (eleKind.kind == "enum") {
                DotRemoveElement(
                    element = smartPointerManager.createSmartPsiElementPointer(element),
                    removeOffsetStart = firstExp.startOffset,
                    removeOffsetEnd = firstExp.endOffset,
                    enumType = firstExp.text,
                    fullText = element.text
                )
            } else {
                null
            }
        }
    }

    private data class KindAndType(val type: String,val kind: String)
    private fun getElementKind(element: PsiElement): KindAndType? {
        val file = element.containingFile.virtualFile ?: return null
        val result = dartService.analysis_getHover(file, element.textOffset)
        if (result.isNotEmpty()) {
            val r = result[0]
            val t =r.staticType
            return KindAndType(t ?: "",r.elementKind)
        }
        return null
    }

    override fun dispose() {
        dartService.removeAnalysisServerListener(this)
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    override fun computedAnalyzedFiles(p0: List<String?>?) {
        val files: List<String> = p0?.filterNotNull() ?: emptyList()
        logger.info("完成的分析文件:${files}")

    }

    override fun computedAvailableSuggestions(
        p0: List<AvailableSuggestionSet?>,
        p1: IntArray
    ) {
    }

    override fun computedCompletion(
        p0: String?,
        p1: Int,
        p2: Int,
        p3: List<CompletionSuggestion?>?,
        p4: List<IncludedSuggestionSet?>?,
        p5: List<String?>?,
        p6: List<IncludedSuggestionRelevanceTag?>?,
        p7: Boolean,
        p8: String?
    ) {
    }

    override fun computedErrors(
        p0: String?,
        p1: List<AnalysisError?>?
    ) {
    }

    override fun computedHighlights(
        p0: String?,
        p1: List<HighlightRegion?>?
    ) {
    }

    override fun computedImplemented(
        p0: String?,
        p1: List<ImplementedClass?>?,
        p2: List<ImplementedMember?>?
    ) {
    }

    override fun computedLaunchData(p0: String?, p1: String?, p2: Array<out String?>?) {
    }

    override fun computedNavigation(
        p0: String?,
        p1: List<NavigationRegion?>?
    ) {
    }

    override fun computedOccurrences(
        p0: String?,
        p1: List<Occurrences?>?
    ) {
    }

    override fun computedOutline(p0: String?, p1: Outline?) {
    }

    override fun computedOverrides(
        p0: String?,
        p1: List<OverrideMember?>?
    ) {
    }

    override fun computedClosingLabels(
        p0: String?,
        p1: List<ClosingLabel?>?
    ) {
    }

    override fun computedSearchResults(
        p0: String?,
        p1: List<SearchResult?>?,
        p2: Boolean
    ) {
    }

    override fun flushedResults(p0: List<String?>?) {
    }

    override fun requestError(p0: RequestError?) {
    }

    override fun serverConnected(p0: String?) {
        logger.info("分析服务器已连接:$p0")
//        startCheck()
    }

    override fun serverError(p0: Boolean, p1: String?, p2: String?) {
    }

    override fun serverIncompatibleVersion(p0: String?) {
    }

    override fun serverStatus(
        p0: AnalysisStatus?,
        p1: PubStatus?
    ) {
    }

    override fun computedExistingImports(
        p0: String?,
        p1: Map<String?, Map<String?, Set<String?>?>?>?
    ) {
    }
}