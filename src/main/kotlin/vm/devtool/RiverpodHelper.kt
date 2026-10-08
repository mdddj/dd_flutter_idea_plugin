package vm.devtool

import vm.VmService
import vm.element.*
import vm.logging.Logging

/**
 * 负责与运行中的 Dart 应用内的 `RiverpodDevtool.instance` 通信。
 *
 * 参考官方 riverpod_devtool 的 `vm_service/eval.dart`、`frames.dart`、`elements.dart`：
 * - 按帧拉取事件并解析 ProviderMeta / OriginMeta
 * - 事件记录求值路径（statePath / notifierPath），按需读取 state 对象
 * - 按帧累积 ElementState（状态 + 依赖关系）
 */
object RiverpodHelper {

    private const val RIVERPOD_LIBRARY_URI = "package:riverpod/src/framework.dart"
    private const val RIVERPOD_FALLBACK_URI = "package:riverpod/riverpod.dart"

    /** 单帧最大解析事件数，避免极端情况下卡死 UI */
    private const val MAX_EVENTS_PER_FRAME = 1000

    private val logger = Logging.getLogger()

    fun getRiverpodEval(vm: VmService): EvalOnDartLibrary? {
        return try {
            EvalOnDartLibrary(RIVERPOD_LIBRARY_URI, vm)
        } catch (e: Exception) {
            try {
                EvalOnDartLibrary(RIVERPOD_FALLBACK_URI, vm)
            } catch (e2: Exception) {
                null
            }
        }
    }

    suspend fun checkAvailable(eval: EvalOnDartLibrary, vm: VmService): Boolean {
        return try {
            val ref = eval.safeEval(vm.getMainIsolateId(), "RiverpodDevtool.instance != null")
            val instance = eval.getInstance(vm.getMainIsolateId(), ref)
            instance.getValueAsString() == "true"
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Riverpod devtool not available: ${e.message}")
            }
            false
        }
    }

    suspend fun getFrameCount(eval: EvalOnDartLibrary, vm: VmService): Int {
        val ref = eval.safeEval(vm.getMainIsolateId(), "RiverpodDevtool.instance.frames.length")
        val instance = eval.getInstance(vm.getMainIsolateId(), ref)
        return instance.getValueAsString()?.toIntOrNull() ?: 0
    }

    /**
     * 拉取 [startIndex] 之后的新帧（增量刷新用）。
     */
    suspend fun getNewFrames(
        eval: EvalOnDartLibrary,
        vm: VmService,
        startIndex: Int
    ): List<RiverpodFrame> {
        val count = getFrameCount(eval, vm)
        if (count <= startIndex) return emptyList()
        val frames = mutableListOf<RiverpodFrame>()
        for (i in startIndex until count) {
            val frame = parseFrame(eval, vm, i) ?: continue
            frames.add(frame)
        }
        return frames
    }

    /**
     * 全量拉取所有帧。
     */
    suspend fun getAllFrames(eval: EvalOnDartLibrary, vm: VmService): List<RiverpodFrame> {
        return getNewFrames(eval, vm, 0)
    }

    private suspend fun parseFrame(
        eval: EvalOnDartLibrary,
        vm: VmService,
        frameIndex: Int
    ): RiverpodFrame? {
        val isolateId = vm.getMainIsolateId()

        val timestamp = try {
            val ref = eval.safeEval(
                isolateId,
                "RiverpodDevtool.instance.frames[$frameIndex].timestamp.millisecondsSinceEpoch"
            )
            eval.getInstance(isolateId, ref).getValueAsString()?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Skip frame $frameIndex (timestamp): ${e.message}")
            }
            return null
        }

        val eventCount = try {
            val ref = eval.safeEval(
                isolateId,
                "RiverpodDevtool.instance.frames[$frameIndex].events.length"
            )
            eval.getInstance(isolateId, ref).getValueAsString()?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Skip frame $frameIndex (events): ${e.message}")
            }
            return null
        }

        val events = mutableListOf<RiverpodEvent>()
        for (j in 0 until minOf(eventCount, MAX_EVENTS_PER_FRAME)) {
            try {
                val event = parseEvent(eval, vm, frameIndex, j)
                if (event != null) events.add(event)
            } catch (e: Exception) {
                if (!isControlFlowException(e)) {
                    logger.logInformation("Skip event [$frameIndex][$j]: ${e.message}")
                }
            }
        }

        return RiverpodFrame(index = frameIndex, timestamp = timestamp, events = events)
    }

    /**
     * 解析单个事件，返回事件类型、ProviderMeta、state/notifier 求值路径。
     */
    private suspend fun parseEvent(
        eval: EvalOnDartLibrary,
        vm: VmService,
        frameIndex: Int,
        eventIndex: Int
    ): RiverpodEvent? {
        val isolateId = vm.getMainIsolateId()
        val eventRef = eval.safeEval(
            isolateId,
            "RiverpodDevtool.instance.frames[$frameIndex].events[$eventIndex]"
        )
        val eventInst = eval.getInstance(isolateId, eventRef)
        val typeName = eventInst.getClassRef().getName()
        val eventType = RiverpodEventType.fromString(typeName)

        if (eventType is RiverpodEventType.Unknown) {
            return RiverpodEvent(type = eventType, providerMeta = null)
        }

        val basePath = "RiverpodDevtool.instance.frames[$frameIndex].events[$eventIndex]"

        if (eventType == RiverpodEventType.ProviderDependencyChange) {
            return parseDependencyChangeEvent(eval, vm, eventInst, basePath)
        }

        val providerMeta = parseProviderMetaFromEvent(eval, vm, eventInst)

        val statePath = when (eventType) {
            RiverpodEventType.ProviderElementAdd -> "$basePath.state.state"
            RiverpodEventType.ProviderElementUpdate -> "$basePath.next.state"
            else -> null
        }
        val notifierPath = when (eventType) {
            RiverpodEventType.ProviderElementAdd,
            RiverpodEventType.ProviderElementUpdate -> "$basePath.notifier?.state"
            else -> null
        }

        return RiverpodEvent(
            type = eventType,
            providerMeta = providerMeta,
            statePath = statePath,
            notifierPath = notifierPath
        )
    }

    private suspend fun parseDependencyChangeEvent(
        eval: EvalOnDartLibrary,
        vm: VmService,
        eventInst: Instance,
        basePath: String
    ): RiverpodEvent {
        val isolateId = vm.getMainIsolateId()
        val elementId = fieldStringValue(eval, vm, eventInst, "elementId")
        val parents = mutableSetOf<String>()

        try {
            val dependenciesField = eventInst.getFields()
                ?.find { it.getDecl()?.getName() == "dependencies" }
                ?.getValue()
            if (dependenciesField != null) {
                val depsInst = eval.getInstance(isolateId, dependenciesField)
                val elements = depsInst.getElements() ?: emptyList()
                for (e in elements) {
                    val id = e.getValueAsString()
                    if (!id.isNullOrBlank()) parents.add(id)
                }
            }
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to parse dependency change event: ${e.message}")
            }
        }

        return RiverpodEvent(
            type = RiverpodEventType.ProviderDependencyChange,
            providerMeta = null,
            dependencyElementId = elementId,
            dependencyParents = parents
        )
    }

    /**
     * 读取事件中记录的 state 对象。
     */
    suspend fun getStateInstance(
        eval: EvalOnDartLibrary,
        vm: VmService,
        statePath: String?
    ): Instance? {
        if (statePath.isNullOrBlank()) return null
        val isolateId = vm.getMainIsolateId()
        return try {
            val ref = eval.safeEval(isolateId, statePath)
            if (ref.isNull()) return null
            eval.getInstance(isolateId, ref)
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to read state via '$statePath': ${e.message}")
            }
            null
        }
    }

    private suspend fun parseProviderMetaFromEvent(
        eval: EvalOnDartLibrary,
        vm: VmService,
        eventInstance: Instance
    ): RiverpodProviderMeta? {
        val isolateId = vm.getMainIsolateId()
        val fields = eventInstance.getFields() ?: return null

        val providerField = fields.find { it.getDecl()?.getName() == "provider" }?.getValue()
            ?: return null

        val providerInst = try {
            eval.getInstance(isolateId, providerField)
        } catch (e: Exception) {
            return null
        }

        return parseProviderMeta(eval, vm, providerInst)
    }

    private suspend fun parseProviderMeta(
        eval: EvalOnDartLibrary,
        vm: VmService,
        providerInst: Instance
    ): RiverpodProviderMeta {
        val isolateId = vm.getMainIsolateId()
        val providerFields = providerInst.getFields() ?: emptyList()

        fun stringField(name: String): String {
            return providerFields.find { it.getDecl()?.getName() == name }
                ?.getValue()?.getValueAsString() ?: ""
        }

        val id = stringField("id")
        val arg = stringField("argToStringValue")
        val hashValue = stringField("hashValue")
        val containerId = stringField("containerId")
        val elementId = stringField("elementId")
        val containerHash = stringField("containerHashValue")
        val creationStackTrace = stringField("creationStackTrace").ifBlank { null }

        val origin = parseOriginMeta(eval, vm, providerInst)

        return RiverpodProviderMeta(
            id = id,
            origin = origin,
            argToStringValue = arg,
            hashValue = hashValue,
            containerId = containerId,
            elementId = elementId,
            containerHashValue = containerHash,
            creationStackTrace = creationStackTrace
        )
    }

    private suspend fun parseOriginMeta(
        eval: EvalOnDartLibrary,
        vm: VmService,
        providerInst: Instance
    ): RiverpodOriginMeta? {
        val isolateId = vm.getMainIsolateId()
        return try {
            val originField = providerInst.getFields()
                ?.find { it.getDecl()?.getName() == "origin" }
                ?.getValue() ?: return null
            val originInst = eval.getInstance(isolateId, originField)
            val originFields = originInst.getFields() ?: return null

            fun stringField(name: String): String {
                return originFields.find { it.getDecl()?.getName() == name }
                    ?.getValue()?.getValueAsString() ?: ""
            }

            RiverpodOriginMeta(
                id = stringField("id"),
                toStringValue = stringField("toStringValue"),
                isFamily = originFields.find { it.getDecl()?.getName() == "isFamily" }
                    ?.getValue()?.getValueAsString() == "true",
                hashValue = stringField("hashValue")
            )
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to parse origin meta: ${e.message}")
            }
            null
        }
    }

    private suspend fun fieldStringValue(
        eval: EvalOnDartLibrary,
        vm: VmService,
        inst: Instance,
        fieldName: String
    ): String {
        val isolateId = vm.getMainIsolateId()
        return try {
            val field = inst.getFields()
                ?.find { it.getDecl()?.getName() == fieldName }
                ?.getValue() ?: return ""
            field.getValueAsString()
                ?: run {
                    // 可能是复杂对象，尝试读取其内部字段
                    val child = eval.getInstance(isolateId, field)
                    child.getFields()
                        ?.find { it.getDecl()?.getName() == "id" }
                        ?.getValue()?.getValueAsString() ?: ""
                }
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to read field $fieldName: ${e.message}")
            }
            ""
        }
    }

    /**
     * 增量累积 ElementState（对应官方 `computeElementsForFrame`）。
     *
     * @param initialAccumulated 之前帧累积到的状态（用于增量刷新）
     * @return Pair(每帧的 state 变更, 每帧的累积快照)
     *   - 每帧的 state 变更：Map<elementId, statePath>，仅包含该帧内新增/更新的 state
     *   - 每帧的累积快照：Map<elementId, ElementState>（到该帧为止的最新状态）
     */
    fun accumulateElements(
        frames: List<RiverpodFrame>,
        initialAccumulated: Map<String, RiverpodElementState> = emptyMap()
    ): Pair<List<Map<String, String>>, List<Map<String, RiverpodElementState>>> {
        val accumulated = initialAccumulated.toMutableMap()
        val changes = mutableListOf<Map<String, String>>()
        val snapshots = mutableListOf<Map<String, RiverpodElementState>>()

        for (frame in frames) {
            val frameChanges = mutableMapOf<String, String>()
            for (event in frame.events) {
                when (event.type) {
                    RiverpodEventType.ProviderElementAdd,
                    RiverpodEventType.ProviderElementUpdate -> {
                        val meta = event.providerMeta ?: continue
                        val elementId = meta.elementId
                        if (elementId.isBlank()) continue
                        frameChanges[elementId] = event.statePath ?: ""
                        val previous = accumulated[elementId]
                        accumulated[elementId] = RiverpodElementState(
                            provider = meta,
                            statePath = event.statePath,
                            notifierPath = event.notifierPath,
                            parents = previous?.parents ?: emptySet(),
                            children = previous?.children ?: emptySet()
                        )
                    }
                    // Dispose 时官方会保留 state，这里同样保留
                    RiverpodEventType.ProviderElementDispose -> Unit
                    RiverpodEventType.ProviderDependencyChange -> {
                        val elementId = event.dependencyElementId ?: continue
                        val previous = accumulated[elementId] ?: continue
                        accumulated[elementId] = previous.copy(
                            parents = event.dependencyParents,
                            children = previous.children
                        )
                    }
                    else -> Unit
                }
            }
            changes.add(frameChanges)
            snapshots.add(accumulated.toMap())
        }
        return changes to snapshots
    }

    /**
     * 聚合所有帧，生成 provider 列表。
     * 同一个 Origin 下的多个 element（family 的不同参数）会共享 origin 信息。
     */
    fun extractProviders(frames: List<RiverpodFrame>): List<RiverpodProviderInfo> {
        val providerMap = linkedMapOf<String, RiverpodProviderInfo>()

        for (frame in frames) {
            for ((eventIndex, event) in frame.events.withIndex()) {
                val meta = event.providerMeta ?: continue
                if (event.type !is RiverpodEventType.ProviderElementAdd &&
                    event.type !is RiverpodEventType.ProviderElementUpdate &&
                    event.type !is RiverpodEventType.ProviderElementDispose
                ) {
                    continue
                }
                val elementId = meta.elementId.ifBlank { meta.id }
                if (elementId.isBlank()) continue

                val existing = providerMap[elementId]
                if (existing == null || frame.index >= existing.frameIndex) {
                    val status = when (event.type) {
                        RiverpodEventType.ProviderElementAdd -> RiverpodProviderStatus.Added
                        RiverpodEventType.ProviderElementUpdate -> RiverpodProviderStatus.Modified
                        RiverpodEventType.ProviderElementDispose -> RiverpodProviderStatus.Disposed
                        else -> null
                    }
                    providerMap[elementId] = RiverpodProviderInfo(
                        origin = meta.origin,
                        elementId = elementId,
                        name = meta.origin?.toStringValue?.ifBlank { "Provider" } ?: "Provider",
                        arg = meta.argToStringValue,
                        containerId = meta.containerId,
                        containerHash = meta.containerHashValue,
                        hashValue = meta.hashValue,
                        lastEventType = event.type,
                        status = status,
                        frameIndex = frame.index,
                        eventIndex = eventIndex,
                        hasState = event.hasState,
                        creationStackTrace = meta.creationStackTrace
                    )
                }
            }
        }
        return providerMap.values.toList()
    }
}
