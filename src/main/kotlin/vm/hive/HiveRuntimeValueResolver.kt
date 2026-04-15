package vm.hive

import com.google.gson.JsonPrimitive
import vm.VmService
import vm.devtool.EvalOnDartLibrary
import vm.element.BoundField
import vm.element.Instance
import vm.element.InstanceKind
import vm.element.InstanceRef
import vm.getObject
import vm.retrieveFullStringValue
import java.util.Base64

class HiveRuntimeValueResolver(
    private val vmService: VmService,
) {
    private val hiveConnectEval = EvalOnDartLibrary("package:hive_ce/src/connect/hive_connect.dart", vmService)

    suspend fun resolveBoxValue(isolateId: String, boxName: String, key: Any): Any? {
        val boxLiteral = toDartLiteral(boxName) ?: return null
        val keyLiteral = toDartLiteral(key) ?: return null
        val expression = """
            (() {
              final box = _boxes[$boxLiteral];
              if (box == null) return null;
              final value = box.get($keyLiteral);
              if (value is Future) return null;
              return value;
            })()
        """.trimIndent()

        val instanceRef = runCatching {
            hiveConnectEval.safeEval(isolateId, expression)
        }.getOrNull() ?: return null

        if (instanceRef.isNull()) {
            return null
        }
        return decodeRef(isolateId, instanceRef, depth = 0, path = linkedSetOf())
    }

    private suspend fun decodeRef(
        isolateId: String,
        ref: InstanceRef,
        depth: Int,
        path: LinkedHashSet<String>,
    ): Any? {
        if (ref.isNull()) return null

        val id = ref.getId()
        if (id.isBlank()) {
            return ref.getValueAsString() ?: "{${ref.getClassRef().getName()}}"
        }
        if (!path.add(id)) {
            return "[Circular ${ref.getClassRef().getName()}]"
        }

        return try {
            val instance = vmService.getObject(isolateId, id)
            if (instance == null) {
                ref.getValueAsString() ?: "{${ref.getClassRef().getName()}}"
            } else {
                decodeInstance(isolateId, instance, depth, path)
            }
        } finally {
            path.remove(id)
        }
    }

    private suspend fun decodeInstance(
        isolateId: String,
        instance: Instance,
        depth: Int,
        path: LinkedHashSet<String>,
    ): Any? {
        if (depth >= MaxDepth) {
            return instance.getValueAsString() ?: "{${instance.getClassRef().getName()}}"
        }

        return when (instance.getKind()) {
            InstanceKind.Null -> null
            InstanceKind.Bool -> instance.getValueAsString()?.toBooleanStrictOrNull() ?: false
            InstanceKind.Int -> instance.getValueAsString()?.toLongOrNull() ?: instance.getValueAsString()
            InstanceKind.Double -> instance.getValueAsString()?.toDoubleOrNull() ?: instance.getValueAsString()
            InstanceKind.String -> vmService.retrieveFullStringValue(isolateId, instance) ?: instance.getValueAsString()
            InstanceKind.List -> instance.getElements()
                ?.map { decodeRef(isolateId, it, depth + 1, path) }
                ?: emptyList<Any?>()

            InstanceKind.Set -> instance.getElements()
                ?.map { decodeRef(isolateId, it, depth + 1, path) }
                ?.toSet()
                ?: emptySet<Any?>()

            InstanceKind.Map -> linkedMapOf<Any?, Any?>().apply {
                instance.getAssociations()?.forEach { association ->
                    val key = association.getKey()?.let { decodeRef(isolateId, it, depth + 1, path) }
                    val value = association.getValue()?.let { decodeRef(isolateId, it, depth + 1, path) }
                    put(key, value)
                }
            }

            InstanceKind.Uint8ClampedList,
            InstanceKind.Uint8List,
            InstanceKind.Uint16List,
            InstanceKind.Uint32List,
            InstanceKind.Uint64List,
            InstanceKind.Int8List,
            InstanceKind.Int16List,
            InstanceKind.Int32List,
            InstanceKind.Int64List,
            InstanceKind.Float32List,
            InstanceKind.Float64List,
            InstanceKind.Int32x4List,
            InstanceKind.Float32x4List,
            InstanceKind.Float64x2List,
            -> decodeTypedData(instance)

            InstanceKind.PlainInstance,
            InstanceKind.Record,
            -> decodeObject(isolateId, instance, depth, path)

            else -> instance.getValueAsString() ?: "{${instance.getClassRef().getName()}}"
        }
    }

    private suspend fun decodeObject(
        isolateId: String,
        instance: Instance,
        depth: Int,
        path: LinkedHashSet<String>,
    ): Any {
        val fields = instance.getFields()?.toList().orEmpty()
        val fieldNames = fields.mapNotNull(BoundField::getName)
        val enumValue = instance.getEnumValue()
        if (enumValue != null && fieldNames.all { it == "_name" || it == "index" }) {
            return HiveRawEnum(instance.getClassRef().getName(), enumValue)
        }

        val decodedFields = fields.mapNotNull { field ->
            val fieldName = field.getName() ?: return@mapNotNull null
            val fieldValue = field.getValue()?.let { decodeRef(isolateId, it, depth + 1, path) }
            HiveRawField(fieldName, fieldValue)
        }

        if (decodedFields.isEmpty()) {
            return instance.getValueAsString() ?: "{${instance.getClassRef().getName()}}"
        }
        return HiveRawObject(instance.getClassRef().getName(), decodedFields)
    }

    private fun decodeTypedData(instance: Instance): Any {
        val bytes = instance.getBytes() ?: return instance.getValueAsString() ?: "[Bytes]"
        return runCatching {
            Base64.getDecoder().decode(bytes)
        }.getOrElse {
            instance.getValueAsString() ?: "[Bytes]"
        }
    }

    private fun toDartLiteral(value: Any?): String? = when (value) {
        null -> "null"
        is String -> JsonPrimitive(value).toString()
        is Number, is Boolean -> value.toString()
        else -> null
    }

    private companion object {
        const val MaxDepth = 4
    }
}
