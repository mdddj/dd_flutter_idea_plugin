package shop.itbug.flutterx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
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
import com.jetbrains.lang.dart.psi.DartShorthandExpression
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import shop.itbug.flutterx.util.MyFileUtil
import kotlin.coroutines.CoroutineContext

data class DotRemoveElement(
    val element: SmartPsiElementPointer<PsiElement>,
    val removedElement: SmartPsiElementPointer<PsiElement>,
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
class DotMigrateService(val project: Project) : Disposable, CoroutineScope {
    private val job = SupervisorJob()
    private val smartPointerManager = SmartPointerManager.getInstance(project)
    private val logger = thisLogger()
    private val dartService = DartAnalysisServerService.getInstance(project)
    private var _cachedElements = MutableStateFlow<MutableList<DotRemoveElement>>(mutableListOf())
    val cachedElements = _cachedElements.asStateFlow()

    // UI 状态变量
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private var _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    private var _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress = _scanProgress.asStateFlow()
    private var _currentAnalysisFile = MutableStateFlow<String?>(null)
    val currentAnalysisFile = _currentAnalysisFile.asStateFlow()

    // 当前扫描任务的 Job，用于取消
    private var currentScanJob: Job? = null

    fun removeElement(element: DotRemoveElement) {
        val newList = _cachedElements.value.toMutableList()
        newList.remove(element)
        _cachedElements.value = newList
    }

    fun cleanElements() {
        _cachedElements.value = mutableListOf()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun cancelScan() {
        currentScanJob?.cancel()
        currentScanJob = null
        _isLoading.value = false
        _scanProgress.value = null
        _currentAnalysisFile.value = null
    }

    suspend fun refreshScan(
        onProgress: (ScanProgress) -> Unit = {},
        onElementFound: (DotRemoveElement) -> Unit = {}
    ): List<DotRemoveElement> {
        // 取消之前的扫描任务
        cancelScan()

        _isLoading.value = true
        _errorMessage.value = null
        _scanProgress.value = null
        _currentAnalysisFile.value = null

        logger.info("开始刷新扫描所有枚举类型")
        _cachedElements.value.clear()

        currentScanJob = launch {
            try {
                _cachedElements.value.addAll(startScanProjectFiles(onProgress, onElementFound))
                logger.info("刷新扫描完成: ${_cachedElements.value.size}个")
            } catch (e: Exception) {
                if (e is CancellationException) {
                    logger.info("扫描被取消")
                    throw e
                }
                _errorMessage.value = "Scan error: ${e.message}"
                logger.warn("扫描失败", e)
            } finally {
                _isLoading.value = false
                _scanProgress.value = null
                _currentAnalysisFile.value = null
                currentScanJob = null
            }
        }

        currentScanJob?.join() // 等待任务完成
        return _cachedElements.value.toList()
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
            }.filter {
                PsiTreeUtil.findChildOfType(it, DartShorthandExpression::class.java) == null
                        && PsiTreeUtil.findChildOfType(it, DartReferenceExpression::class.java) != null
            }.filterNotNull()
        }
        logger.info("所有参数节点:${allNamedArguments.size}")

        val totalCount = allNamedArguments.size
        var processedCount = 0
        var foundCount = 0
        val results = mutableListOf<DotRemoveElement>()

        val semaphore = Semaphore(permits = 1)
        logger.info("开始循环遍历分析枚举赋值")

        withContext(Dispatchers.IO) {
            allNamedArguments.map { argument: DartNamedArgument ->
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
                            val progress = ScanProgress(totalCount, processedCount, foundCount)
                            _scanProgress.value = progress
                            onProgress(progress)
                        }
                        delay(1) //需要添加延迟,dart分析服务器会检测到 null
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
            if (!element.isValid) return@readAction null
            _currentAnalysisFile.value = element.containingFile.name

            val argNameEle = element.parameterReferenceExpression
            val argNameInfo = getElementKind(argNameEle)
            val argType = argNameInfo?.type
            if (argType == "dynamic" || argType == "Object" || argType == "Object?" || argType == "T") {
                return@readAction null
            }


            val expression = element.expression
            val firstExp: DartReferenceExpression =
                expression.firstChild as? DartReferenceExpression ?: return@readAction null
            val childrenRefSize = PsiTreeUtil.countChildrenOfType(firstExp, DartReferenceExpression::class.java)
            if (childrenRefSize >= 2) {
                return@readAction null
            }
            if (expression.lastChild !is DartReferenceExpression) return@readAction null
            val referenceEleIsEnum = getElementKind(firstExp)?.kind == "enum"
            if (referenceEleIsEnum) {
                DotRemoveElement(
                    element = smartPointerManager.createSmartPsiElementPointer(element),
                    removedElement = smartPointerManager.createSmartPsiElementPointer(firstExp),
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

    private data class KindAndType(val type: String, val kind: String)

    private fun getElementKind(element: PsiElement): KindAndType? {
        val file = element.containingFile.virtualFile ?: return null
        val result = dartService.analysis_getHover(file, element.textOffset)
        if (result.isNotEmpty()) {
            val r = result[0]
            val t = r.staticType
            return KindAndType(t ?: "", r.elementKind ?: "")
        }
        return null
    }

    fun stop() {
        cancelScan()
        logger.info("停止所有任务")
    }

    override fun dispose() {
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default


    companion object {
        fun getInstance(project: Project) = project.service<DotMigrateService>()
    }
}