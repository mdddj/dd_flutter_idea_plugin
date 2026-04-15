package vm.hive

import java.nio.file.Path
import java.time.Duration
import java.time.Instant

sealed class HiveAsyncState<out T> {
    data object Loading : HiveAsyncState<Nothing>()
    data class Data<T>(val data: T) : HiveAsyncState<T>()
    data class Error(val error: Throwable) : HiveAsyncState<Nothing>()
}

enum class HiveExtensionStatus {
    Loading,
    Ready,
    Unavailable,
}

enum class HiveConnectAction(val method: String) {
    ListBoxes("ext.hive_ce.listBoxes"),
    GetBoxFrames("ext.hive_ce.getBoxFrames"),
    LoadValue("ext.hive_ce.loadValue"),
}

enum class HiveConnectEvent(val event: String) {
    BoxRegistered("ext.hive_ce.boxRegistered"),
    BoxUnregistered("ext.hive_ce.boxUnregistered"),
    BoxEvent("ext.hive_ce.boxEvent"),
}

data class HiveSchema(
    val nextTypeId: Int,
    val types: Map<String, HiveSchemaType>,
)

data class HiveSchemaType(
    val typeId: Int,
    val nextIndex: Int,
    val fields: Map<String, HiveSchemaField>,
)

data class HiveSchemaField(
    val index: Int,
)

data class HiveSchemaRegistry(
    val types: Map<String, HiveSchemaType>,
    val sourceFiles: List<Path>,
)

data class HiveRawEnum(
    val name: String,
    val value: String,
) {
    override fun toString(): String = "$name.$value"
}

data class HiveRawField(
    val name: String,
    val value: Any?,
) {
    override fun toString(): String = "$name: ${value.toHiveSearchableString()}"
}

data class HiveRawObject(
    val name: String,
    val fields: List<HiveRawField>,
) {
    override fun toString(): String = "$name($fields)"
}

data class HiveListRef(
    val boxName: String,
    val keys: List<Any>,
) {
    override fun toString(): String = "HiveList(box=$boxName, keys=$keys)"
}

data class HiveDateTimeValue(
    val instant: Instant,
    val isUtc: Boolean,
) {
    override fun toString(): String = if (isUtc) {
        instant.toString()
    } else {
        instant.atZone(java.time.ZoneId.systemDefault()).toString()
    }
}

data class HiveDurationValue(
    val duration: Duration,
) {
    override fun toString(): String = duration.toString()
}

data class HiveInspectorFrame(
    val key: Any,
    val value: Any?,
    val lazy: Boolean = false,
    val deleted: Boolean = false,
)

data class HiveBoxState(
    val name: String,
    val frames: LinkedHashMap<Any, HiveInspectorFrame> = linkedMapOf(),
    val open: Boolean = true,
    val loaded: Boolean = false,
)

data class HiveInspectorState(
    val status: HiveExtensionStatus = HiveExtensionStatus.Loading,
    val boxes: Map<String, HiveBoxState> = emptyMap(),
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val schemaFiles: Int = 0,
    val statusMessage: String = "",
)

data class HiveTableEntry(
    val key: Any,
    val value: Any?,
    val lazy: Boolean = false,
)

fun String.toHiveAnonymousFieldIndexOrNull(): Int? {
    if (!startsWith("#")) return null
    return removePrefix("#").toIntOrNull()
}

fun HiveRawObject.isAnonymousHiveObject(): Boolean {
    return name.startsWith("Type#") || fields.any { it.name.toHiveAnonymousFieldIndexOrNull() != null }
}

fun Any?.needsHiveRuntimeResolution(): Boolean = when (this) {
    is HiveRawObject -> name.startsWith("Type#") || fields.any { it.name.startsWith("#") || it.value.needsHiveRuntimeResolution() }
    is List<*> -> any { it.needsHiveRuntimeResolution() }
    is Set<*> -> any { it.needsHiveRuntimeResolution() }
    is Map<*, *> -> entries.any { it.key.needsHiveRuntimeResolution() || it.value.needsHiveRuntimeResolution() }
    else -> false
}

fun Any?.supportsHiveRuntimeLookupKey(): Boolean = this is String || this is Number || this is Boolean

fun Any?.toHiveSummaryText(): String = when (this) {
    null -> "null"
    is ByteArray -> "[${size} bytes] ${toHiveHexPreview()}"
    is List<*> -> if (isEmpty()) "[Empty List]" else "[List ${size}]"
    is Set<*> -> if (isEmpty()) "[Empty Set]" else "[Set ${size}]"
    is Map<*, *> -> if (isEmpty()) "[Empty Map]" else "[Map ${size}]"
    is HiveListRef -> "[HiveList ${boxName} • ${keys.size}]"
    is HiveRawObject -> if (isAnonymousHiveObject()) {
        "[${name} • ${fields.size} fields]"
    } else {
        "{$name}"
    }
    is HiveRawEnum -> toString()
    else -> toString()
}

fun Any?.toHiveSearchableString(): String = when (this) {
    null -> "null"
    is ByteArray -> joinToString(prefix = "[", postfix = "]")
    is List<*> -> joinToString(prefix = "[", postfix = "]") { it.toHiveSearchableString() }
    is Set<*> -> joinToString(prefix = "[", postfix = "]") { it.toHiveSearchableString() }
    is Map<*, *> -> entries.joinToString(prefix = "{", postfix = "}") {
        "${it.key.toHiveSearchableString()}: ${it.value.toHiveSearchableString()}"
    }
    is HiveListRef -> "HiveList($boxName) ${keys.joinToString(", ") { it.toHiveSearchableString() }}"
    is HiveRawObject -> "$name ${fields.joinToString(", ") { "${it.name}=${it.value.toHiveSearchableString()}" }}"
    is HiveRawEnum -> toString()
    else -> toString()
}

fun Any?.toHiveChildEntries(): List<HiveTableEntry>? = when (this) {
    is List<*> -> mapIndexed { index, value -> HiveTableEntry(index, value) }
    is Set<*> -> toList().mapIndexed { index, value -> HiveTableEntry(index, value) }
    is Map<*, *> -> entries.map { HiveTableEntry(it.key ?: "null", it.value) }
    is HiveRawObject -> fields.map { HiveTableEntry(it.name, it.value) }
    is HiveListRef -> keys.mapIndexed { index, value -> HiveTableEntry(index, value) }
    else -> null
}

fun ByteArray.toHiveHexPreview(maxBytes: Int = 8): String {
    if (isEmpty()) return "(empty)"
    val preview = take(maxBytes).joinToString(" ") { byte ->
        "%02X".format(byte.toInt() and 0xFF)
    }
    return if (size > maxBytes) "$preview ..." else preview
}
