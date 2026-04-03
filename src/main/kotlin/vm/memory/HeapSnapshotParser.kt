package vm.memory

import java.io.ByteArrayOutputStream
import kotlin.math.max

data class HeapSnapshotClassTotals(
    val key: String,
    val className: String,
    val libraryUri: String,
    val instances: Long,
    val bytes: Long,
)

data class ParsedHeapSnapshot(
    val isolateName: String,
    val objectCount: Int,
    val classTotalsByKey: Map<String, HeapSnapshotClassTotals>,
    val identityHashesByClass: Map<String, Set<Int>>,
    val hasIdentityHashCodes: Boolean,
    val trackedIdentityObjectCount: Int,
)

/**
 * Parses Dart VM heap snapshot binary chunks emitted on the HeapSnapshot stream.
 *
 * The binary layout is ported from `vm_service` (Dart) snapshot_graph.dart.
 */
object HeapSnapshotParser {
    private const val MAGIC_HEADER = "dartheap"

    private const val TAG_NO_DATA = 0L
    private const val TAG_NULL_DATA = 1L
    private const val TAG_BOOL_DATA = 2L
    private const val TAG_INT_DATA = 3L
    private const val TAG_DOUBLE_DATA = 4L
    private const val TAG_LATIN1_DATA = 5L
    private const val TAG_UTF16_DATA = 6L
    private const val TAG_LENGTH_DATA = 7L
    private const val TAG_NAME_DATA = 8L

    fun parse(chunks: List<ByteArray>): ParsedHeapSnapshot {
        require(chunks.isNotEmpty()) { "Heap snapshot chunks are empty." }
        val stream = SnapshotReadStream(chunks)

        val header = stream.readAscii(MAGIC_HEADER.length)
        require(header == MAGIC_HEADER) { "Invalid heap snapshot header: $header" }

        stream.readVarInt() // flags
        val isolateName = stream.readUtf8()
        stream.readVarInt() // shallow size
        stream.readVarInt() // capacity
        stream.readVarInt() // external size

        val classInfoById = readClasses(stream)
        val objectSection = readObjects(stream, classInfoById)
        if (!stream.atEnd) {
            skipExternalProperties(stream)
        }
        val identitySection = readIdentityHashCodes(stream, objectSection.classKeyByObjectId)

        val classTotalsByKey = objectSection.classTotalsByKey.values
            .filter { it.instances > 0L }
            .associateBy { it.key }

        return ParsedHeapSnapshot(
            isolateName = isolateName,
            objectCount = objectSection.classTotalsByKey.values.sumOf { max(0L, it.instances) }.toInt(),
            classTotalsByKey = classTotalsByKey,
            identityHashesByClass = identitySection.identityHashesByClass,
            hasIdentityHashCodes = identitySection.sectionPresent,
            trackedIdentityObjectCount = identitySection.trackedObjectCount,
        )
    }

    private fun readClasses(stream: SnapshotReadStream): Map<Int, ClassInfo> {
        val classCount = stream.readVarInt().toInt()
        val result = HashMap<Int, ClassInfo>(classCount + 1)
        result[0] = ClassInfo(id = 0, name = "Root", libraryUri = "")

        for (classId in 1..classCount) {
            stream.readVarInt() // flags
            val name = stream.readUtf8()
            stream.readUtf8() // libraryName (unused)
            val libraryUri = stream.readUtf8()
            stream.readUtf8() // reserved

            val fieldCount = stream.readVarInt().toInt()
            repeat(fieldCount) {
                stream.readVarInt() // field flags
                stream.readVarInt() // field index
                stream.readUtf8() // field name
                stream.readUtf8() // reserved
            }

            result[classId] = ClassInfo(id = classId, name = name, libraryUri = libraryUri)
        }
        return result
    }

    private fun readObjects(
        stream: SnapshotReadStream,
        classInfoById: Map<Int, ClassInfo>,
    ): ObjectSectionResult {
        stream.readVarInt() // referenceCount
        val objectCount = stream.readVarInt().toInt()

        val totals = linkedMapOf<String, HeapSnapshotClassTotals>()
        val classKeyByObjectId = arrayOfNulls<String>(objectCount + 1)

        for (objectId in 1..objectCount) {
            val classId = stream.readVarInt().toInt()
            val shallowSize = stream.readVarInt()

            skipObjectData(stream)

            val referencesCount = stream.readVarInt().toInt()
            repeat(referencesCount) {
                stream.readVarInt() // child object id
            }

            if (classId <= 0) continue
            val classInfo = classInfoById[classId] ?: continue
            val classKey = "${classInfo.libraryUri}::${classInfo.name}"
            classKeyByObjectId[objectId] = classKey
            val previous = totals[classKey]
            totals[classKey] =
                if (previous == null) {
                    HeapSnapshotClassTotals(
                        key = classKey,
                        className = classInfo.name,
                        libraryUri = classInfo.libraryUri,
                        instances = 1L,
                        bytes = shallowSize,
                    )
                } else {
                    previous.copy(
                        instances = previous.instances + 1L,
                        bytes = previous.bytes + shallowSize,
                    )
                }
        }

        return ObjectSectionResult(
            classTotalsByKey = totals,
            classKeyByObjectId = classKeyByObjectId,
        )
    }

    private fun skipExternalProperties(stream: SnapshotReadStream) {
        if (stream.atEnd) return
        val propertyCount = stream.readVarInt().toInt()
        repeat(propertyCount) {
            stream.readVarInt() // objectId
            stream.readVarInt() // externalSize
            stream.skipUtf8() // property name
        }
    }

    private fun readIdentityHashCodes(
        stream: SnapshotReadStream,
        classKeyByObjectId: Array<String?>,
    ): IdentitySectionResult {
        if (stream.atEnd) {
            return IdentitySectionResult(
                identityHashesByClass = emptyMap(),
                sectionPresent = false,
                trackedObjectCount = 0,
            )
        }

        val identityByClass = linkedMapOf<String, MutableSet<Int>>()
        var trackedObjectCount = 0
        var readAnyValue = false

        for (objectId in 1 until classKeyByObjectId.size) {
            if (stream.atEnd) break
            val identityHash = stream.readVarInt().toInt()
            readAnyValue = true
            if (identityHash == 0) continue
            val classKey = classKeyByObjectId[objectId] ?: continue
            identityByClass.getOrPut(classKey) { linkedSetOf() }.add(identityHash)
            trackedObjectCount++
        }

        return IdentitySectionResult(
            identityHashesByClass = identityByClass,
            sectionPresent = readAnyValue,
            trackedObjectCount = trackedObjectCount,
        )
    }

    private fun skipObjectData(stream: SnapshotReadStream) {
        when (val tag = stream.readVarInt()) {
            TAG_NO_DATA,
            TAG_NULL_DATA,
            -> Unit

            TAG_BOOL_DATA -> stream.skip(1)
            TAG_INT_DATA -> stream.readVarInt()
            TAG_DOUBLE_DATA -> stream.skip(8)

            TAG_LATIN1_DATA -> {
                stream.readVarInt() // logical length
                val dataLength = stream.readVarInt().toInt()
                stream.skip(dataLength)
            }

            TAG_UTF16_DATA -> {
                stream.readVarInt() // logical length
                val codeUnitLength = stream.readVarInt().toInt()
                stream.skip(codeUnitLength * 2)
            }

            TAG_LENGTH_DATA -> stream.readVarInt()
            TAG_NAME_DATA -> stream.skipUtf8()

            else -> error("Unsupported heap object data tag: $tag")
        }
    }

    private data class ClassInfo(
        val id: Int,
        val name: String,
        val libraryUri: String,
    )

    private data class ObjectSectionResult(
        val classTotalsByKey: MutableMap<String, HeapSnapshotClassTotals>,
        val classKeyByObjectId: Array<String?>,
    )

    private data class IdentitySectionResult(
        val identityHashesByClass: Map<String, Set<Int>>,
        val sectionPresent: Boolean,
        val trackedObjectCount: Int,
    )
}

private class SnapshotReadStream(chunks: List<ByteArray>) {
    private val data: ByteArray = merge(chunks)
    private var offset = 0
    val atEnd: Boolean
        get() = offset >= data.size

    fun readVarInt(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val part = readByte().toInt() and 0xFF
            result = result or ((part and 0x7F).toLong() shl shift)
            if ((part and 0x80) == 0) return result
            shift += 7
            require(shift <= 63) { "Invalid varint: too many bytes." }
        }
    }

    fun readAscii(length: Int): String {
        val bytes = readBytes(length)
        return bytes.toString(Charsets.US_ASCII)
    }

    fun readUtf8(): String {
        val len = readVarInt().toInt()
        if (len <= 0) return ""
        val bytes = readBytes(len)
        return bytes.toString(Charsets.UTF_8)
    }

    fun skipUtf8() {
        val len = readVarInt().toInt()
        if (len > 0) skip(len)
    }

    fun skip(bytes: Int) {
        require(bytes >= 0) { "Skip bytes must be non-negative." }
        val newOffset = offset + bytes
        require(newOffset <= data.size) { "Heap snapshot stream truncated." }
        offset = newOffset
    }

    private fun readByte(): Byte {
        require(offset < data.size) { "Heap snapshot stream truncated." }
        return data[offset++]
    }

    private fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "Length must be non-negative." }
        val end = offset + length
        require(end <= data.size) { "Heap snapshot stream truncated." }
        val result = data.copyOfRange(offset, end)
        offset = end
        return result
    }

    private fun merge(chunks: List<ByteArray>): ByteArray {
        val out = ByteArrayOutputStream(chunks.sumOf { it.size })
        for (chunk in chunks) {
            out.write(chunk)
        }
        return out.toByteArray()
    }
}

internal fun decodeBase64OrNull(value: String): ByteArray? {
    return runCatching {
        java.util.Base64.getDecoder().decode(value)
    }.getOrNull()
}
