package vm.hive

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant

class HiveRawObjectReader(
    types: Map<String, HiveSchemaType>,
    private val buffer: ByteArray,
) {
    private val rawTypes = types
    private val typeMappings = rawTypes.entries.associateBy { calculateTypeId(it.value.typeId, internal = false) }
    private var offset = 0

    fun read(typeId: Int? = null): Any? {
        val resolvedTypeId = typeId ?: readTypeId()
        if (isInternalTypeId(resolvedTypeId)) {
            return readInternal(resolvedTypeId)
        }

        val isEnum = readByte() == 1
        val dataLength = readInt32()
        val payload = readBytes(dataLength)

        val type = typeMappings[resolvedTypeId]
        if (type == null) {
            if (resolvedTypeId == 233 && payload.size == 8) {
                val nested = HiveRawObjectReader(rawTypes, payload)
                val totalMinutes = (nested.readInternal(FrameValueType.IntT) as? Long)?.toInt() ?: return payload
                val hour = totalMinutes / 60
                val minute = totalMinutes % 60
                return "%d:%02d".format(hour, minute)
            }
            return decodeUnknownCustomType(resolvedTypeId, isEnum, payload)
        }

        return runCatching {
            val nested = HiveRawObjectReader(rawTypes, payload)
            val fieldByIndex = type.value.fields.entries.associateBy { it.value.index }
            if (isEnum) {
                val index = nested.readByte()
                val enumField = fieldByIndex[index]
                HiveRawEnum(type.key, enumField?.key ?: "#$index")
            } else {
                val fieldCount = nested.readByte()
                val fields = buildList {
                    repeat(fieldCount) {
                        val index = nested.readByte()
                        val fieldName = fieldByIndex[index]?.key ?: "#$index"
                        add(HiveRawField(fieldName, nested.read()))
                    }
                }
                HiveRawObject(type.key, fields)
            }
        }.getOrElse {
            payload
        }
    }

    private fun decodeUnknownCustomType(typeId: Int, isEnum: Boolean, payload: ByteArray): Any {
        return runCatching {
            val nested = HiveRawObjectReader(rawTypes, payload)
            val fallbackTypeName = "Type#$typeId"
            if (isEnum) {
                HiveRawEnum(fallbackTypeName, "#${nested.readByte()}")
            } else {
                val fieldCount = nested.readByte()
                val fields = buildList {
                    repeat(fieldCount) {
                        val index = nested.readByte()
                        add(HiveRawField("#$index", nested.read()))
                    }
                }
                HiveRawObject(fallbackTypeName, fields)
            }
        }.getOrElse {
            payload
        }
    }

    private fun readInternal(typeId: Int): Any? = when (typeId) {
        FrameValueType.NullT -> null
        FrameValueType.IntT -> readInt()
        FrameValueType.DoubleT -> readDouble()
        FrameValueType.BoolT -> readBool()
        FrameValueType.StringT -> readString()
        FrameValueType.ByteListT -> readByteList()
        FrameValueType.IntListT -> readIntList()
        FrameValueType.DoubleListT -> readDoubleList()
        FrameValueType.BoolListT -> readBoolList()
        FrameValueType.StringListT -> readStringList()
        FrameValueType.ListT -> readList()
        FrameValueType.MapT -> readMap()
        FrameValueType.HiveListT -> readHiveList()
        FrameValueType.IntSetT -> readIntList().toSet()
        FrameValueType.DoubleSetT -> readDoubleList().toSet()
        FrameValueType.StringSetT -> readStringList().toSet()
        FrameValueType.DateTimeT -> HiveDateTimeValue(Instant.ofEpochMilli(readInt()), isUtc = false)
        FrameValueType.BigIntT -> {
            val len = readByte()
            BigInteger(readString(len))
        }
        FrameValueType.DateTimeWithTimezoneT -> {
            val millis = readInt()
            val isUtc = readBool()
            HiveDateTimeValue(Instant.ofEpochMilli(millis), isUtc = isUtc)
        }
        FrameValueType.SetT -> readList().toSet()
        FrameValueType.DurationT -> HiveDurationValue(Duration.ofMillis(readInt()))
        else -> error("Unsupported Hive type id: $typeId")
    }

    private fun readByte(): Int {
        ensureAvailable(1)
        return buffer[offset++].toInt() and 0xFF
    }

    private fun readWord(): Int {
        ensureAvailable(2)
        val value = (buffer[offset].toInt() and 0xFF) or ((buffer[offset + 1].toInt() and 0xFF) shl 8)
        offset += 2
        return value
    }

    private fun readInt32(): Int {
        ensureAvailable(4)
        val value = (buffer[offset].toInt() and 0xFF) or
                ((buffer[offset + 1].toInt() and 0xFF) shl 8) or
                ((buffer[offset + 2].toInt() and 0xFF) shl 16) or
                (buffer[offset + 3].toInt() shl 24)
        offset += 4
        return value
    }

    private fun readUInt32(): Long {
        ensureAvailable(4)
        val value = (buffer[offset].toLong() and 0xFF) or
                ((buffer[offset + 1].toLong() and 0xFF) shl 8) or
                ((buffer[offset + 2].toLong() and 0xFF) shl 16) or
                ((buffer[offset + 3].toLong() and 0xFF) shl 24)
        offset += 4
        return value
    }

    private fun readInt(): Long = readDouble().toLong()

    private fun readDouble(): Double {
        val bits = readLongLittleEndian()
        return Double.fromBits(bits)
    }

    private fun readLongLittleEndian(): Long {
        ensureAvailable(8)
        var value = 0L
        repeat(8) { index ->
            value = value or ((buffer[offset + index].toLong() and 0xFF) shl (index * 8))
        }
        offset += 8
        return value
    }

    private fun readBool(): Boolean = readByte() > 0

    private fun readString(length: Int? = null): String {
        val byteCount = length ?: readUInt32().toInt()
        val bytes = readBytes(byteCount)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun readBytes(length: Int): ByteArray {
        ensureAvailable(length)
        val bytes = buffer.copyOfRange(offset, offset + length)
        offset += length
        return bytes
    }

    private fun readByteList(): ByteArray {
        val length = readUInt32().toInt()
        return readBytes(length)
    }

    private fun readIntList(): List<Long> {
        val length = readUInt32().toInt()
        return List(length) { readInt() }
    }

    private fun readDoubleList(): List<Double> {
        val length = readUInt32().toInt()
        return List(length) { readDouble() }
    }

    private fun readBoolList(): List<Boolean> {
        val length = readUInt32().toInt()
        return List(length) { readBool() }
    }

    private fun readStringList(): List<String> {
        val length = readUInt32().toInt()
        return List(length) { readString() }
    }

    private fun readList(): List<Any?> {
        val length = readUInt32().toInt()
        return List(length) { read() }
    }

    private fun readMap(): Map<Any?, Any?> {
        val length = readUInt32().toInt()
        return buildMap(length) {
            repeat(length) {
                put(read(), read())
            }
        }
    }

    private fun readKey(): Any {
        return when (val keyType = readByte()) {
            FrameKeyType.UIntT -> readUInt32()
            FrameKeyType.Utf8StringT -> {
                val byteCount = readByte()
                val bytes = readBytes(byteCount)
                String(bytes, StandardCharsets.UTF_8)
            }

            else -> error("Unsupported Hive key type: $keyType")
        }
    }

    private fun readHiveList(): HiveListRef {
        val length = readUInt32().toInt()
        val boxNameLength = readByte()
        val boxName = String(readBytes(boxNameLength), StandardCharsets.UTF_8)
        val keys = List(length) { readKey() }
        return HiveListRef(boxName, keys)
    }

    private fun readTypeId(): Int {
        val typeId = readByte()
        return if (typeId == FrameValueType.TypeIdExtension) {
            readWord()
        } else {
            typeId
        }
    }

    private fun ensureAvailable(length: Int) {
        if (offset + length > buffer.size) {
            error("Not enough bytes available. offset=$offset length=$length size=${buffer.size}")
        }
    }

    companion object {
        private const val ReservedTypeIds = 32
        private const val ReservedExtendedTypeIds = 64
        private const val MaxTypeId = 255

        private fun calculateTypeId(typeId: Int, internal: Boolean): Int {
            return if (internal) {
                if (typeId > ReservedTypeIds - 1) {
                    typeId - ReservedTypeIds + MaxTypeId + 1
                } else {
                    typeId
                }
            } else {
                if (typeId > MaxTypeId - ReservedTypeIds) {
                    typeId + ReservedTypeIds + ReservedExtendedTypeIds
                } else {
                    typeId + ReservedTypeIds
                }
            }
        }

        private fun isInternalTypeId(typeId: Int): Boolean {
            val firstExtendedInternalTypeId = MaxTypeId + 1
            return (typeId in 0 until ReservedTypeIds) ||
                    (typeId in firstExtendedInternalTypeId until (firstExtendedInternalTypeId + ReservedExtendedTypeIds))
        }
    }
}

private object FrameKeyType {
    const val UIntT = 0
    const val Utf8StringT = 1
}

private object FrameValueType {
    const val NullT = 0
    const val IntT = 1
    const val DoubleT = 2
    const val BoolT = 3
    const val StringT = 4
    const val ByteListT = 5
    const val IntListT = 6
    const val DoubleListT = 7
    const val BoolListT = 8
    const val StringListT = 9
    const val ListT = 10
    const val MapT = 11
    const val HiveListT = 12
    const val IntSetT = 13
    const val DoubleSetT = 14
    const val StringSetT = 15
    const val DateTimeT = 16
    const val BigIntT = 17
    const val DateTimeWithTimezoneT = 18
    const val SetT = 19
    const val DurationT = 20
    const val TypeIdExtension = 21
}
