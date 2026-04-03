package vm.memory

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vm.VmService
import vm.getAllocationProfileOrNull
import vm.getAllocationTracesOrNull
import vm.getClassListOrNull
import vm.getVm
import vm.getInstancesOrNull
import vm.getRetainingPathOrNull
import vm.requestHeapSnapshotNow
import vm.setTraceClassAllocationEnabled
import vm.element.AllocationProfile
import vm.element.ElementList
import vm.element.Event
import vm.element.IsolateRef
import vm.element.MemoryUsage
import vm.element.ObjRef
import kotlin.math.abs

enum class HeapSnapshotStatus {
    Requested,
    Streaming,
    Completed,
    Failed,
}

data class MemoryUsageSummary(
    val heapUsage: Long,
    val heapCapacity: Long,
    val externalUsage: Long,
)

data class MemoryChartPoint(
    val timestampMillis: Long,
    val heapUsage: Long,
    val heapCapacity: Long,
    val externalUsage: Long,
)

data class ProfileClassStat(
    val classId: String,
    val className: String,
    val libraryUri: String,
    val instancesCurrent: Int,
    val instancesAccumulated: Int,
    val bytesCurrent: Long,
    val accumulatedBytes: Long,
)

data class HeapSnapshotRecord(
    val id: Int,
    val isolateId: String,
    val startedAtMillis: Long,
    val chunkCount: Int,
    val payloadBytes: Long,
    val status: HeapSnapshotStatus,
    val isLastChunk: Boolean,
    val heapParsed: Boolean = false,
    val parsedClassCount: Int = 0,
    val error: String? = null,
)

data class TraceClassItem(
    val classId: String,
    val className: String,
    val libraryUri: String,
)

data class SnapshotClassDiff(
    val classId: String,
    val className: String,
    val libraryUri: String,
    val beforeInstances: Long,
    val afterInstances: Long,
    val deltaInstances: Long,
    val beforeBytes: Long,
    val afterBytes: Long,
    val deltaBytes: Long,
    val identityTrackedBefore: Int,
    val identityTrackedAfter: Int,
    val identityAdded: Int,
    val identityRemoved: Int,
    val addedIdentityHashes: List<Int>,
    val removedIdentityHashes: List<Int>,
)

data class SnapshotDiffSummary(
    val isolateId: String,
    val beforeSnapshotId: Int,
    val afterSnapshotId: Int,
    val beforeTakenAtMillis: Long,
    val afterTakenAtMillis: Long,
    val totalBeforeBytes: Long,
    val totalAfterBytes: Long,
    val totalDeltaBytes: Long,
    val totalDeltaInstances: Long,
    val beforeSource: String,
    val afterSource: String,
    val identityBased: Boolean,
    val totalIdentityTrackedBefore: Int,
    val totalIdentityTrackedAfter: Int,
    val totalIdentityAdded: Int,
    val totalIdentityRemoved: Int,
    val classDiffs: List<SnapshotClassDiff>,
)

private data class SnapshotProfileData(
    val isolateId: String,
    val classStatsById: Map<String, ProfileClassStat>,
    val identityHashesByClass: Map<String, Set<Int>>,
    val hasIdentityHashCodes: Boolean,
    val source: String,
)

class DartVmMemoryController(
    private val vmService: VmService,
    parentScope: CoroutineScope,
) : VmService.VmEventListener {
    private val job: Job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isolates = MutableStateFlow<List<IsolateRef>>(emptyList())
    val isolates: StateFlow<List<IsolateRef>> = _isolates.asStateFlow()

    private val _selectedIsolateId = MutableStateFlow<String?>(null)
    val selectedIsolateId: StateFlow<String?> = _selectedIsolateId.asStateFlow()

    private val _profileLoading = MutableStateFlow(false)
    val profileLoading: StateFlow<Boolean> = _profileLoading.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    private val _memoryUsage = MutableStateFlow<MemoryUsageSummary?>(null)
    val memoryUsage: StateFlow<MemoryUsageSummary?> = _memoryUsage.asStateFlow()

    private val _profileStats = MutableStateFlow<List<ProfileClassStat>>(emptyList())
    val profileStats: StateFlow<List<ProfileClassStat>> = _profileStats.asStateFlow()

    private val _memoryChartPoints = MutableStateFlow<List<MemoryChartPoint>>(emptyList())
    val memoryChartPoints: StateFlow<List<MemoryChartPoint>> = _memoryChartPoints.asStateFlow()

    private val _memoryGcEvents = MutableStateFlow<List<Long>>(emptyList())
    val memoryGcEvents: StateFlow<List<Long>> = _memoryGcEvents.asStateFlow()

    private val _snapshotInProgress = MutableStateFlow(false)
    val snapshotInProgress: StateFlow<Boolean> = _snapshotInProgress.asStateFlow()

    private val _snapshots = MutableStateFlow<List<HeapSnapshotRecord>>(emptyList())
    val snapshots: StateFlow<List<HeapSnapshotRecord>> = _snapshots.asStateFlow()

    private val _snapshotDiffSummary = MutableStateFlow<SnapshotDiffSummary?>(null)
    val snapshotDiffSummary: StateFlow<SnapshotDiffSummary?> = _snapshotDiffSummary.asStateFlow()

    private val _selectedDiffBeforeSnapshotId = MutableStateFlow<Int?>(null)
    val selectedDiffBeforeSnapshotId: StateFlow<Int?> = _selectedDiffBeforeSnapshotId.asStateFlow()

    private val _selectedDiffAfterSnapshotId = MutableStateFlow<Int?>(null)
    val selectedDiffAfterSnapshotId: StateFlow<Int?> = _selectedDiffAfterSnapshotId.asStateFlow()

    private val _traceClassLoading = MutableStateFlow(false)
    val traceClassLoading: StateFlow<Boolean> = _traceClassLoading.asStateFlow()

    private val _traceLoading = MutableStateFlow(false)
    val traceLoading: StateFlow<Boolean> = _traceLoading.asStateFlow()

    private val _traceError = MutableStateFlow<String?>(null)
    val traceError: StateFlow<String?> = _traceError.asStateFlow()

    private val _traceClasses = MutableStateFlow<List<TraceClassItem>>(emptyList())
    val traceClasses: StateFlow<List<TraceClassItem>> = _traceClasses.asStateFlow()

    private val _selectedTraceClassId = MutableStateFlow<String?>(null)
    val selectedTraceClassId: StateFlow<String?> = _selectedTraceClassId.asStateFlow()

    private val _tracedClassIds = MutableStateFlow<Set<String>>(emptySet())
    val tracedClassIds: StateFlow<Set<String>> = _tracedClassIds.asStateFlow()

    private val _traceSampleCount = MutableStateFlow<Int?>(null)
    val traceSampleCount: StateFlow<Int?> = _traceSampleCount.asStateFlow()

    private val _traceInstanceCount = MutableStateFlow<Int?>(null)
    val traceInstanceCount: StateFlow<Int?> = _traceInstanceCount.asStateFlow()

    private val _firstInstanceId = MutableStateFlow<String?>(null)
    val firstInstanceId: StateFlow<String?> = _firstInstanceId.asStateFlow()

    private val _retainingPathLines = MutableStateFlow<List<String>>(emptyList())
    val retainingPathLines: StateFlow<List<String>> = _retainingPathLines.asStateFlow()

    private var snapshotSeq = 0
    private var activeSnapshotId: Int? = null
    private val snapshotProfiles = mutableMapOf<Int, SnapshotProfileData>()
    private val snapshotBinaryChunks = mutableMapOf<Int, MutableList<ByteArray>>()
    private var memorySamplingJob: Job? = null

    private val memorySampleIntervalMs = 2000L
    private val memorySampleHistoryLimit = 180
    private val memoryGcEventHistoryLimit = 256

    init {
        vmService.addEventListener(this)
        vmService.streamListen(VmService.HEAPSNAPSHOT_STREAM_ID)
        vmService.streamListen(VmService.GC_STREAM_ID)
        scope.launch {
            refreshAll()
        }
    }

    override fun onVmEvent(streamId: String, event: Event) {
        scope.launch {
            when (streamId) {
                VmService.HEAPSNAPSHOT_STREAM_ID -> handleHeapSnapshotEvent(event)
                VmService.GC_STREAM_ID -> handleGcEvent(event)
            }
        }
    }

    fun dispose() {
        vmService.removeEventListener(this)
        vmService.streamCancel(VmService.HEAPSNAPSHOT_STREAM_ID)
        vmService.streamCancel(VmService.GC_STREAM_ID)
        memorySamplingJob?.cancel()
        scope.cancel()
    }

    fun refreshAllAsync() {
        scope.launch { refreshAll() }
    }

    fun selectIsolate(isolateId: String) {
        if (isolateId == _selectedIsolateId.value) return
        _selectedIsolateId.value = isolateId
        resetMemoryChart()
        restartMemorySampling()
        _statusMessage.value = "Switched isolate to $isolateId"
        recalculateSnapshotDiff()
        refreshProfileAsync()
        refreshTraceClassesAsync()
    }

    fun refreshProfileAsync(gc: Boolean = false) {
        scope.launch { refreshProfile(gc = gc) }
    }

    fun refreshTraceClassesAsync() {
        scope.launch { refreshTraceClasses() }
    }

    fun selectTraceClass(classId: String) {
        _selectedTraceClassId.value = classId
        inspectSelectedTraceClassAsync()
    }

    fun inspectSelectedTraceClassAsync() {
        scope.launch { inspectSelectedTraceClass() }
    }

    fun setClassTracingAsync(classId: String, enabled: Boolean) {
        scope.launch {
            val isolateId = _selectedIsolateId.value ?: return@launch
            val ok = vmService.setTraceClassAllocationEnabled(isolateId, classId, enabled)
            if (ok) {
                _tracedClassIds.update { previous ->
                    if (enabled) previous + classId else previous - classId
                }
                _statusMessage.value =
                    if (enabled) "Tracing enabled for $classId" else "Tracing disabled for $classId"
            } else {
                _traceError.value = "Failed to update tracing for $classId"
            }
        }
    }

    fun requestHeapSnapshotAsync() {
        scope.launch {
            val isolateId = _selectedIsolateId.value ?: return@launch
            if (_snapshotInProgress.value) {
                _statusMessage.value = "Heap snapshot is already running."
                return@launch
            }
            val id = ++snapshotSeq
            activeSnapshotId = id
            _snapshotInProgress.value = true
            _snapshots.update {
                listOf(
                    HeapSnapshotRecord(
                        id = id,
                        isolateId = isolateId,
                        startedAtMillis = System.currentTimeMillis(),
                        chunkCount = 0,
                        payloadBytes = 0,
                        status = HeapSnapshotStatus.Requested,
                        isLastChunk = false,
                    ),
                ) + it
            }
            snapshotBinaryChunks[id] = mutableListOf()

            val requested = vmService.requestHeapSnapshotNow(isolateId)
            if (!requested) {
                updateSnapshot(id) {
                    it.copy(
                        status = HeapSnapshotStatus.Failed,
                        error = "requestHeapSnapshot RPC failed",
                    )
                }
                snapshotProfiles.remove(id)
                snapshotBinaryChunks.remove(id)
                activeSnapshotId = null
                _snapshotInProgress.value = false
                _statusMessage.value = "Failed to request heap snapshot."
            } else {
                _statusMessage.value = "Heap snapshot requested."
            }
        }
    }

    fun clearSnapshots() {
        _snapshots.value = emptyList()
        snapshotProfiles.clear()
        snapshotBinaryChunks.clear()
        _snapshotDiffSummary.value = null
        _selectedDiffBeforeSnapshotId.value = null
        _selectedDiffAfterSnapshotId.value = null
        activeSnapshotId = null
        _snapshotInProgress.value = false
    }

    fun selectDiffBeforeSnapshot(snapshotId: Int) {
        _selectedDiffBeforeSnapshotId.value = snapshotId
        recalculateSnapshotDiff()
    }

    fun selectDiffAfterSnapshot(snapshotId: Int) {
        _selectedDiffAfterSnapshotId.value = snapshotId
        recalculateSnapshotDiff()
    }

    private suspend fun refreshAll() {
        refreshIsolates()
        restartMemorySampling()
        refreshProfile(gc = false)
        refreshTraceClasses()
    }

    private suspend fun refreshIsolates() {
        runCatching { vmService.updateMainIsolateId() }
        val vm = runCatching { vmService.getVm() }.getOrNull()
        if (vm == null) {
            _statusMessage.value = "Failed to load VM isolate list."
            return
        }

        val all = vm.getIsolates().toMutableList()
        val nonSystem = all.filterNot { it.getIsSystemIsolate() }
        val candidates = if (nonSystem.isNotEmpty()) nonSystem else all

        _isolates.value = candidates
        if (candidates.isEmpty()) {
            _selectedIsolateId.value = null
            return
        }

        val selected = _selectedIsolateId.value
        if (selected != null && candidates.any { it.getId() == selected }) return

        val mainIsolateId = vmService.getMainIsolateId()
        val mainCandidate = candidates.firstOrNull { it.getId() == mainIsolateId }?.getId()
            ?: candidates.firstOrNull { it.getName() == "main" }?.getId()
        _selectedIsolateId.value = mainCandidate ?: candidates.first().getId()
    }

    private suspend fun refreshProfile(gc: Boolean) {
        val isolateId = _selectedIsolateId.value ?: return
        _profileLoading.value = true
        _profileError.value = null
        try {
            val profile = vmService.getAllocationProfileOrNull(
                isolateId = isolateId,
                gc = if (gc) true else null,
            )
            if (profile == null) {
                _profileError.value = "AllocationProfile is unavailable."
                _profileStats.value = emptyList()
                return
            }

            val memoryUsage = profile.getMemoryUsage()
            val usageSummary = memoryUsage.toSummary()
            _memoryUsage.value = usageSummary
            appendMemoryChartPoint(usageSummary)

            val stats = profile.toProfileStats()
            _profileStats.value = stats.sortedByDescending { it.bytesCurrent }
            _statusMessage.value = "Profile loaded (${stats.size} classes)."
        } catch (t: Throwable) {
            _profileError.value = t.message ?: "Failed to load AllocationProfile."
            _profileStats.value = emptyList()
        } finally {
            _profileLoading.value = false
        }
    }

    private fun restartMemorySampling() {
        memorySamplingJob?.cancel()
        val isolateId = _selectedIsolateId.value ?: return
        memorySamplingJob = scope.launch {
            while (isActive && _selectedIsolateId.value == isolateId) {
                runCatching { sampleMemoryUsage() }
                delay(memorySampleIntervalMs)
            }
        }
    }

    private suspend fun sampleMemoryUsage() {
        val isolateId = _selectedIsolateId.value ?: return
        val profile = vmService.getAllocationProfileOrNull(isolateId = isolateId) ?: return
        val usageSummary = profile.getMemoryUsage().toSummary()
        _memoryUsage.value = usageSummary
        appendMemoryChartPoint(usageSummary)
    }

    private fun appendMemoryChartPoint(summary: MemoryUsageSummary) {
        val point = MemoryChartPoint(
            timestampMillis = System.currentTimeMillis(),
            heapUsage = summary.heapUsage,
            heapCapacity = summary.heapCapacity,
            externalUsage = summary.externalUsage,
        )
        _memoryChartPoints.update { previous ->
            val merged = previous + point
            if (merged.size <= memorySampleHistoryLimit) merged else merged.takeLast(memorySampleHistoryLimit)
        }
    }

    private fun resetMemoryChart() {
        _memoryChartPoints.value = emptyList()
        _memoryGcEvents.value = emptyList()
    }

    private fun appendGcEvent(timestampMillis: Long) {
        _memoryGcEvents.update { previous ->
            val merged = previous + timestampMillis
            if (merged.size <= memoryGcEventHistoryLimit) merged else merged.takeLast(memoryGcEventHistoryLimit)
        }
    }

    private fun handleGcEvent(event: Event) {
        val selectedIsolateId = _selectedIsolateId.value ?: return
        val eventIsolateId = event.getIsolate()?.getId()
        if (eventIsolateId != null && eventIsolateId != selectedIsolateId) return
        val timestamp = event.getTimestamp().takeIf { it > 0L } ?: System.currentTimeMillis()
        appendGcEvent(timestamp)
    }

    private suspend fun refreshTraceClasses() {
        val isolateId = _selectedIsolateId.value ?: return
        _traceClassLoading.value = true
        _traceError.value = null
        try {
            val classList = vmService.getClassListOrNull(isolateId)
            if (classList == null) {
                _traceError.value = "ClassList is unavailable."
                _traceClasses.value = emptyList()
                return
            }
            val rows = mutableListOf<TraceClassItem>()
            for (item in classList.getClasses()) {
                rows += TraceClassItem(
                    classId = item.getId(),
                    className = item.getName(),
                    libraryUri = item.getLibrary()?.getUri() ?: "",
                )
            }
            val sorted = rows.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.className })
            _traceClasses.value = sorted
            if (_selectedTraceClassId.value == null || sorted.none { it.classId == _selectedTraceClassId.value }) {
                _selectedTraceClassId.value = sorted.firstOrNull()?.classId
            }
            _statusMessage.value = "Trace class list loaded (${sorted.size})."
        } catch (t: Throwable) {
            _traceError.value = t.message ?: "Failed to load class list."
            _traceClasses.value = emptyList()
        } finally {
            _traceClassLoading.value = false
        }
    }

    private suspend fun inspectSelectedTraceClass() {
        val isolateId = _selectedIsolateId.value ?: return
        val classId = _selectedTraceClassId.value ?: return
        _traceLoading.value = true
        _traceError.value = null
        try {
            val traces = vmService.getAllocationTracesOrNull(
                isolateId = isolateId,
                classId = classId,
            )
            _traceSampleCount.value = traces?.getSampleCount()

            val instanceSet = vmService.getInstancesOrNull(
                isolateId = isolateId,
                classId = classId,
                limit = 1024,
            )
            _traceInstanceCount.value = instanceSet?.getTotalCount()
            val first = firstInstance(instanceSet)
            _firstInstanceId.value = first

            if (first != null) {
                val path = vmService.getRetainingPathOrNull(
                    isolateId = isolateId,
                    targetId = first,
                    limit = 32,
                )
                _retainingPathLines.value = buildRetainingPathLines(path)
            } else {
                _retainingPathLines.value = emptyList()
            }
        } catch (t: Throwable) {
            _traceError.value = t.message ?: "Failed to inspect trace class."
            _retainingPathLines.value = emptyList()
        } finally {
            _traceLoading.value = false
        }
    }

    private suspend fun handleHeapSnapshotEvent(event: Event) {
        val id = activeSnapshotId ?: return
        val current = _snapshots.value.firstOrNull { it.id == id } ?: return
        val isolateId = event.getIsolate()?.getId()
        if (isolateId != null && isolateId != current.isolateId) return

        val chunkBytes = event.readHeapSnapshotChunkBytes()
        if (chunkBytes != null) {
            snapshotBinaryChunks.getOrPut(id) { mutableListOf() }.add(chunkBytes)
        }
        val payloadBytes = chunkBytes?.size?.toLong()
            ?: event.json.get("chunk")?.asString?.length?.toLong()
            ?: event.json.get("bytes")?.asString?.length?.toLong()
            ?: 0L
        val isLast = event.getLast()

        updateSnapshot(id) {
            it.copy(
                chunkCount = it.chunkCount + 1,
                payloadBytes = it.payloadBytes + payloadBytes,
                status = if (isLast) HeapSnapshotStatus.Completed else HeapSnapshotStatus.Streaming,
                isLastChunk = isLast,
            )
        }
        if (isLast) {
            val capturedProfile = captureHeapProfileForDiff(id, current.isolateId)
            if (capturedProfile != null) {
                snapshotProfiles[id] = capturedProfile
                updateSnapshot(id) {
                    it.copy(
                        heapParsed = capturedProfile.source == "heap_snapshot",
                        parsedClassCount = capturedProfile.classStatsById.size,
                    )
                }
            } else {
                updateSnapshot(id) {
                    val existingError = it.error
                    val captureError = "Heap snapshot parsing and profile fallback both failed."
                    it.copy(
                        error = if (existingError.isNullOrBlank()) captureError else "$existingError | $captureError",
                    )
                }
            }
            snapshotBinaryChunks.remove(id)
            activeSnapshotId = null
            _snapshotInProgress.value = false
            recalculateSnapshotDiff()
            _statusMessage.value = "Heap snapshot stream completed."
        }
    }

    private fun updateSnapshot(id: Int, transform: (HeapSnapshotRecord) -> HeapSnapshotRecord) {
        _snapshots.update { current ->
            current.map { record ->
                if (record.id == id) transform(record) else record
            }
        }
    }

    private fun firstInstance(instanceSet: vm.element.InstanceSet?): String? {
        if (instanceSet == null) return null
        for (instance in instanceSet.getInstances()) {
            return instance.getId()
        }
        return null
    }

    private fun buildRetainingPathLines(path: vm.element.RetainingPath?): List<String> {
        if (path == null) return emptyList()
        val lines = mutableListOf<String>()
        val gcRoot = path.getGcRootType()
        if (!gcRoot.isNullOrBlank()) {
            lines += "GC Root: $gcRoot"
        }
        var index = 1
        for (element in path.getElements()) {
            val value = element.getValue()
            lines += formatRetainingObject(index++, value, element.getParentField())
        }
        return lines
    }

    private fun formatRetainingObject(index: Int, value: ObjRef, parentField: Any?): String {
        val name = value.getName().ifBlank { value.type }
        val fieldPart = parentField?.toString()?.takeIf { it.isNotBlank() }?.let { " via $it" } ?: ""
        return "$index. $name (${value.getId()})$fieldPart"
    }

    private suspend fun captureHeapProfileForDiff(
        snapshotId: Int,
        isolateId: String,
    ): SnapshotProfileData? {
        val parsed = runCatching {
            val chunks = snapshotBinaryChunks[snapshotId]?.toList().orEmpty()
            if (chunks.isEmpty()) null else HeapSnapshotParser.parse(chunks)
        }.getOrNull()

        if (parsed != null) {
            val mapped = parsed.classTotalsByKey.values.associate { totals ->
                totals.key to ProfileClassStat(
                    classId = totals.key,
                    className = totals.className,
                    libraryUri = totals.libraryUri,
                    instancesCurrent = totals.instances.toInt(),
                    instancesAccumulated = totals.instances.toInt(),
                    bytesCurrent = totals.bytes,
                    accumulatedBytes = totals.bytes,
                )
            }
            return SnapshotProfileData(
                isolateId = isolateId,
                classStatsById = mapped,
                identityHashesByClass = parsed.identityHashesByClass,
                hasIdentityHashCodes = parsed.hasIdentityHashCodes,
                source = "heap_snapshot",
            )
        }

        val profile = vmService.getAllocationProfileOrNull(isolateId = isolateId)
        val stats = profile?.toProfileStats() ?: return null
        return SnapshotProfileData(
            isolateId = isolateId,
            classStatsById = stats.associateBy { profileClassKey(it.className, it.libraryUri) },
            identityHashesByClass = emptyMap(),
            hasIdentityHashCodes = false,
            source = "allocation_profile_fallback",
        )
    }

    private fun recalculateSnapshotDiff() {
        val isolateId = _selectedIsolateId.value
        if (isolateId.isNullOrBlank()) {
            _snapshotDiffSummary.value = null
            return
        }
        val completed = _snapshots.value
            .asSequence()
            .filter { it.status == HeapSnapshotStatus.Completed }
            .filter { it.isolateId == isolateId }
            .filter { snapshotProfiles.containsKey(it.id) }
            .sortedBy { it.id }
            .toList()
        if (completed.size < 2) {
            _snapshotDiffSummary.value = null
            _selectedDiffBeforeSnapshotId.value = null
            _selectedDiffAfterSnapshotId.value = null
            return
        }
        val availableIds = completed.map { it.id }.toSet()
        var beforeId = _selectedDiffBeforeSnapshotId.value?.takeIf { availableIds.contains(it) }
        var afterId = _selectedDiffAfterSnapshotId.value?.takeIf { availableIds.contains(it) }

        if (beforeId == null || afterId == null || beforeId == afterId) {
            beforeId = completed[completed.lastIndex - 1].id
            afterId = completed.last().id
        }

        val indexById = completed.mapIndexed { index, record -> record.id to index }.toMap()
        var beforeIndex = indexById[beforeId] ?: (completed.lastIndex - 1)
        var afterIndex = indexById[afterId] ?: completed.lastIndex
        if (beforeIndex >= afterIndex) {
            val minIndex = minOf(beforeIndex, afterIndex)
            val maxIndex = maxOf(beforeIndex, afterIndex)
            beforeIndex = if (minIndex < maxIndex) minIndex else maxOf(0, maxIndex - 1)
            afterIndex = if (minIndex < maxIndex) maxIndex else minOf(completed.lastIndex, beforeIndex + 1)
        }

        val before = completed[beforeIndex]
        val after = completed[afterIndex]
        if (_selectedDiffBeforeSnapshotId.value != before.id) {
            _selectedDiffBeforeSnapshotId.value = before.id
        }
        if (_selectedDiffAfterSnapshotId.value != after.id) {
            _selectedDiffAfterSnapshotId.value = after.id
        }

        val beforeProfile = snapshotProfiles[before.id] ?: run {
            _snapshotDiffSummary.value = null
            return
        }
        val afterProfile = snapshotProfiles[after.id] ?: run {
            _snapshotDiffSummary.value = null
            return
        }
        val identityBased = beforeProfile.hasIdentityHashCodes && afterProfile.hasIdentityHashCodes

        val classIds = (beforeProfile.classStatsById.keys + afterProfile.classStatsById.keys)
        val rows = classIds.mapNotNull { classId ->
            val beforeStat = beforeProfile.classStatsById[classId]
            val afterStat = afterProfile.classStatsById[classId]
            val beforeInstances = beforeStat?.instancesCurrent?.toLong() ?: 0L
            val afterInstances = afterStat?.instancesCurrent?.toLong() ?: 0L
            val beforeBytes = beforeStat?.bytesCurrent ?: 0L
            val afterBytes = afterStat?.bytesCurrent ?: 0L
            val deltaInstances = afterInstances - beforeInstances
            val deltaBytes = afterBytes - beforeBytes
            val beforeHashes = if (identityBased) {
                beforeProfile.identityHashesByClass[classId].orEmpty()
            } else {
                emptySet()
            }
            val afterHashes = if (identityBased) {
                afterProfile.identityHashesByClass[classId].orEmpty()
            } else {
                emptySet()
            }
            val identityTrackedBefore = beforeHashes.size
            val identityTrackedAfter = afterHashes.size
            val addedHashes = if (identityBased) {
                afterHashes.filter { !beforeHashes.contains(it) }.sorted()
            } else {
                emptyList()
            }
            val removedHashes = if (identityBased) {
                beforeHashes.filter { !afterHashes.contains(it) }.sorted()
            } else {
                emptyList()
            }
            val identityAdded = addedHashes.size
            val identityRemoved = removedHashes.size

            if (deltaInstances == 0L &&
                deltaBytes == 0L &&
                identityAdded == 0 &&
                identityRemoved == 0
            ) return@mapNotNull null

            SnapshotClassDiff(
                classId = classId,
                className = afterStat?.className ?: beforeStat?.className ?: classId,
                libraryUri = afterStat?.libraryUri ?: beforeStat?.libraryUri ?: "",
                beforeInstances = beforeInstances,
                afterInstances = afterInstances,
                deltaInstances = deltaInstances,
                beforeBytes = beforeBytes,
                afterBytes = afterBytes,
                deltaBytes = deltaBytes,
                identityTrackedBefore = identityTrackedBefore,
                identityTrackedAfter = identityTrackedAfter,
                identityAdded = identityAdded,
                identityRemoved = identityRemoved,
                addedIdentityHashes = addedHashes,
                removedIdentityHashes = removedHashes,
            )
        }.sortedWith(
            compareByDescending<SnapshotClassDiff> { abs(it.deltaBytes) }
                .thenByDescending { abs(it.deltaInstances) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className },
        )

        val totalBeforeBytes = beforeProfile.classStatsById.values.sumOf { it.bytesCurrent }
        val totalAfterBytes = afterProfile.classStatsById.values.sumOf { it.bytesCurrent }
        val totalBeforeInstances = beforeProfile.classStatsById.values.sumOf { it.instancesCurrent.toLong() }
        val totalAfterInstances = afterProfile.classStatsById.values.sumOf { it.instancesCurrent.toLong() }
        val totalIdentityTrackedBefore = rows.sumOf { it.identityTrackedBefore }
        val totalIdentityTrackedAfter = rows.sumOf { it.identityTrackedAfter }
        val totalIdentityAdded = rows.sumOf { it.identityAdded }
        val totalIdentityRemoved = rows.sumOf { it.identityRemoved }

        _snapshotDiffSummary.value = SnapshotDiffSummary(
            isolateId = isolateId,
            beforeSnapshotId = before.id,
            afterSnapshotId = after.id,
            beforeTakenAtMillis = before.startedAtMillis,
            afterTakenAtMillis = after.startedAtMillis,
            totalBeforeBytes = totalBeforeBytes,
            totalAfterBytes = totalAfterBytes,
            totalDeltaBytes = totalAfterBytes - totalBeforeBytes,
            totalDeltaInstances = totalAfterInstances - totalBeforeInstances,
            beforeSource = beforeProfile.source,
            afterSource = afterProfile.source,
            identityBased = identityBased,
            totalIdentityTrackedBefore = totalIdentityTrackedBefore,
            totalIdentityTrackedAfter = totalIdentityTrackedAfter,
            totalIdentityAdded = totalIdentityAdded,
            totalIdentityRemoved = totalIdentityRemoved,
            classDiffs = rows,
        )
    }

    private fun AllocationProfile.toProfileStats(): List<ProfileClassStat> {
        val stats = mutableListOf<ProfileClassStat>()
        for (member in getMembers()) {
            val classRef = member.getClassRef()
            stats += ProfileClassStat(
                classId = classRef.getId(),
                className = classRef.getName(),
                libraryUri = classRef.getLibrary()?.getUri() ?: "",
                instancesCurrent = member.getInstancesCurrent(),
                instancesAccumulated = member.getInstancesAccumulated(),
                bytesCurrent = member.getBytesCurrent().toLong(),
                accumulatedBytes = member.getAccumulatedSize().toLong(),
            )
        }
        return stats
    }

    private fun MemoryUsage.toSummary(): MemoryUsageSummary {
        return MemoryUsageSummary(
            heapUsage = getHeapUsage().toLong(),
            heapCapacity = getHeapCapacity().toLong(),
            externalUsage = getExternalUsage().toLong(),
        )
    }
}

private fun profileClassKey(className: String, libraryUri: String): String {
    return "${libraryUri}::${className}"
}

private fun Event.readHeapSnapshotChunkBytes(): ByteArray? {
    val json = this.json
    val directBase64 = json.get("dataBase64")?.asString
    if (!directBase64.isNullOrBlank()) {
        decodeBase64OrNull(directBase64)?.let { return it }
    }

    val chunk = json.get("chunk")?.asString
    if (!chunk.isNullOrBlank()) {
        decodeBase64OrNull(chunk)?.let { return it }
        return chunk.toByteArray(Charsets.ISO_8859_1)
    }

    val bytesBase64 = getBytes()
    if (!bytesBase64.isNullOrBlank()) {
        decodeBase64OrNull(bytesBase64)?.let { return it }
    }
    return null
}

private fun <T> ElementList<T>.toMutableList(): MutableList<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        result += item
    }
    return result
}
