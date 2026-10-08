package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import vm.element.IsolateRef
import vm.getAllocationProfileOrNull
import vm.getVm

/**
 * Memory 面板：当前 isolate 的堆用量，以及占用最多的类。
 * 不触发 GC，也不抓 heap snapshot。
 */
class DartVmMemoryMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read the Memory tab summary for one Flutter isolate: heap usage, heap capacity, external memory, and the classes currently using the most bytes. Does not force a GC and does not take a heap snapshot. Omit isolateId to use the main isolate. classQuery filters class name or library URI. limit defaults to 30 and is capped at 100."
    )
    suspend fun flutterx_memory(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Dart isolate id from flutterx_vm_status. Omit it to use the main isolate.") isolateId: String? = null,
        @McpDescription("Case-insensitive match against class name or library URI. Omit it to return the largest classes.") classQuery: String? = null,
        @McpDescription("How many classes to return. Default 30, maximum 100.") limit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val vm = app.vmService.getVm()
        val isolates = vm.getIsolates().toList()
        val target = resolveIsolate(isolates, isolateId)
        val targetId = target.getId() ?: mcpFail("Selected isolate has no id")
        val profile = app.vmService.getAllocationProfileOrNull(targetId)
            ?: mcpFail("Allocation profile is not available for isolate $targetId")
        val usage = profile.getMemoryUsage()
        val count = clampCount(limit, defaultValue = 30, maxValue = 100)
        val token = classQuery?.trim()?.lowercase().orEmpty()
        val matched = profile.getMembers().map { member ->
            val classRef = member.getClassRef()
            ClassRow(
                classId = classRef.getId(),
                className = classRef.getName(),
                libraryUri = classRef.getLibrary()?.getUri().orEmpty(),
                instancesCurrent = member.getInstancesCurrent(),
                instancesAccumulated = member.getInstancesAccumulated(),
                bytesCurrent = member.getBytesCurrent().toLong(),
                accumulatedBytes = member.getAccumulatedSize().toLong(),
            )
        }.filter { row ->
            val matchesQuery = token.isEmpty() ||
                    row.className.lowercase().contains(token) ||
                    row.libraryUri.lowercase().contains(token)
            val hasData = row.instancesCurrent > 0 || row.bytesCurrent > 0L
            matchesQuery && (token.isNotEmpty() || hasData)
        }.sortedByDescending { it.bytesCurrent }
        mapOf(
            "app" to app.toMcpSummary(),
            "isolate" to isolateSummary(target),
            "memory" to linkedMapOf(
                "heapUsageBytes" to usage.getHeapUsage().toLong(),
                "heapCapacityBytes" to usage.getHeapCapacity().toLong(),
                "externalUsageBytes" to usage.getExternalUsage().toLong(),
            ),
            "matchedClasses" to matched.size,
            "returned" to matched.take(count).size,
            "classes" to matched.take(count).map { row ->
                linkedMapOf(
                    "classId" to row.classId,
                    "className" to row.className,
                    "libraryUri" to row.libraryUri,
                    "instancesCurrent" to row.instancesCurrent,
                    "instancesAccumulated" to row.instancesAccumulated,
                    "bytesCurrent" to row.bytesCurrent,
                    "accumulatedBytes" to row.accumulatedBytes,
                )
            },
            "isolates" to isolates.map(::isolateSummary),
        ).toMcpJson()
    }

    private fun resolveIsolate(isolates: List<IsolateRef>, isolateId: String?): IsolateRef {
        if (isolates.isEmpty()) mcpFail("No isolate is running")
        if (!isolateId.isNullOrBlank()) {
            return isolates.firstOrNull { it.getId() == isolateId }
                ?: mcpFail(
                    "Unknown isolateId '$isolateId'. Isolates: " +
                            isolates.joinToString { "${it.getId()} (${it.getName()})" }
                )
        }
        val userIsolates = isolates.filterNot { it.getIsSystemIsolate() }
        return userIsolates.firstOrNull { it.getName() == "main" }
            ?: userIsolates.firstOrNull()
            ?: isolates.first()
    }

    private fun isolateSummary(isolate: IsolateRef): Map<String, Any?> = linkedMapOf(
        "id" to isolate.getId(),
        "name" to isolate.getName(),
        "isSystemIsolate" to isolate.getIsSystemIsolate(),
    )

    private data class ClassRow(
        val classId: String,
        val className: String,
        val libraryUri: String,
        val instancesCurrent: Int,
        val instancesAccumulated: Int,
        val bytesCurrent: Long,
        val accumulatedBytes: Long,
    )
}
