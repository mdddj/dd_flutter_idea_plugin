package vm.devtool

import vm.VmService
import vm.element.Instance
import vm.element.InstanceKind
import vm.element.InstanceRef
import vm.logging.Logging

/**
 * 将 Riverpod 的 state 对象渲染为缩进文本树。
 *
 * 输出格式对齐官方 devtool Inspector 的展示风格，同时保证：
 * - 每个属性/元素独占一行，便于使用 IDE diff 编辑器高亮"哪个属性变了"
 * - 容器（List/Map/Set/Object）展开为多行
 * - 带递归检测与节点预算，避免超深/超大对象卡死
 */
object RiverpodStateRenderer {

    private val logger = Logging.getLogger()

    /** 最大渲染深度 */
    private const val MAX_DEPTH = 8

    /** 最大渲染节点数 */
    private const val MAX_NODES = 1500

    private class Budget {
        var remaining = MAX_NODES
    }

    /**
     * 渲染一个 state 对象为文本。
     */
    suspend fun render(
        eval: EvalOnDartLibrary,
        vm: VmService,
        instance: Instance?
    ): String {
        val sb = StringBuilder(512)
        val visited = HashSet<String>()
        val budget = Budget()
        try {
            appendNode(eval, vm, instance, sb, "", "", visited, budget, 0)
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to render state: ${e.message}")
                sb.append("<error: ${e.message}>")
            }
        }
        return sb.toString().trimEnd('\n')
    }

    private suspend fun appendNode(
        eval: EvalOnDartLibrary,
        vm: VmService,
        instance: Instance?,
        sb: StringBuilder,
        pad: String,
        label: String,
        visited: MutableSet<String>,
        budget: Budget,
        depth: Int
    ) {
        if (instance == null || instance.isNull()) {
            sb.append(pad).append(label).append("null").append('\n')
            return
        }
        if (budget.remaining-- <= 0) {
            sb.append(pad).append(label).append("...").append('\n')
            return
        }
        if (depth > MAX_DEPTH) {
            sb.append(pad).append(label).append("...").append('\n')
            return
        }

        val id = instance.getId()
        if (id.isNotBlank() && !visited.add(id)) {
            sb.append(pad).append(label).append("<recursion>").append('\n')
            return
        }

        try {
            when (instance.getKind()) {
                InstanceKind.String -> {
                    val v = instance.getValueAsString() ?: ""
                    sb.append(pad).append(label).append('"').append(v).append('"').append('\n')
                }
                InstanceKind.Int,
                InstanceKind.Double -> {
                    sb.append(pad).append(label).append(instance.getValueAsString() ?: "0").append('\n')
                }
                InstanceKind.Bool -> {
                    sb.append(pad).append(label).append(instance.getValueAsString() ?: "false").append('\n')
                }
                InstanceKind.Null -> {
                    sb.append(pad).append(label).append("null").append('\n')
                }
                InstanceKind.Type -> {
                    sb.append(pad).append(label).append(instance.getName()).append('\n')
                }
                InstanceKind.List -> {
                    sb.append(pad).append(label).append('[').append('\n')
                    val elements = instance.getElements()
                    if (elements != null) {
                        for ((index, ref) in elements.withIndex()) {
                            appendInlineChild(eval, vm, ref, sb, pad, "[$index] = ", visited, budget, depth)
                        }
                    }
                    sb.append(pad).append(']').append('\n')
                }
                InstanceKind.Map -> {
                    sb.append(pad).append(label).append('{').append('\n')
                    val associations = instance.getAssociations()
                    if (associations != null) {
                        for (assoc in associations) {
                            val keyText = inlineValue(eval, vm, assoc.getKey())
                            appendInlineChild(eval, vm, assoc.getValue(), sb, pad, "$keyText => ", visited, budget, depth)
                        }
                    }
                    sb.append(pad).append('}').append('\n')
                }
                InstanceKind.Set -> {
                    sb.append(pad).append(label).append('{').append('\n')
                    val elements = instance.getElements()
                    if (elements != null) {
                        for (ref in elements) {
                            appendInlineChild(eval, vm, ref, sb, pad, "", visited, budget, depth)
                        }
                    }
                    sb.append(pad).append('}').append('\n')
                }
                InstanceKind.Record -> {
                    sb.append(pad).append(label).append("Record {").append('\n')
                    val fields = instance.getFields()
                    if (fields != null) {
                        for (field in fields) {
                            val name = field.getName() ?: "?"
                            appendInlineChild(eval, vm, field.getValue(), sb, pad, "$name = ", visited, budget, depth)
                        }
                    }
                    sb.append(pad).append('}').append('\n')
                }
                else -> {
                    // PlainInstance 及其余类型：作为对象展开字段
                    val classRef = instance.getClassRef()
                    val typeName = classRef.getName().ifBlank { "Object" }
                    val hash = runCatching { instance.getIdentityHashCode() }.getOrNull()
                    val hashText = if (hash != null && hash != 0) "#${hash.toString(16).take(6)}" else ""
                    sb.append(pad).append(label).append(typeName).append(' ').append(hashText).append(" {").append('\n')
                    val fields = instance.getFields()
                    if (fields != null) {
                        val sorted = fields
                            .filter { it.getDecl()?.isStatic() != true }
                            .sortedBy { it.getDecl()?.getName()?.lowercase() }
                        for (field in sorted) {
                            val name = field.getDecl()?.getName() ?: field.getName() ?: "?"
                            appendInlineChild(eval, vm, field.getValue(), sb, pad, "$name = ", visited, budget, depth)
                        }
                    }
                    sb.append(pad).append('}').append('\n')
                }
            }
        } finally {
            if (id.isNotBlank()) visited.remove(id)
        }
    }

    /**
     * 渲染一个"子节点"。
     */
    private suspend fun appendInlineChild(
        eval: EvalOnDartLibrary,
        vm: VmService,
        ref: InstanceRef?,
        sb: StringBuilder,
        parentPad: String,
        label: String,
        visited: MutableSet<String>,
        budget: Budget,
        depth: Int
    ) {
        if (ref == null) {
            sb.append(parentPad).append("  ").append(label).append("null").append('\n')
            return
        }
        val childInstance = try {
            eval.getInstance(vm.getMainIsolateId(), ref)
        } catch (e: Exception) {
            if (!isControlFlowException(e)) {
                logger.logInformation("Failed to read child instance: ${e.message}")
            }
            null
        }
        if (childInstance == null) {
            sb.append(parentPad).append("  ").append(label).append("<expired>").append('\n')
            return
        }
        appendNode(eval, vm, childInstance, sb, parentPad + "  ", label, visited, budget, depth + 1)
    }

    /**
     * 计算一个值的单行摘要（用于 Map 的 key 显示等）。
     */
    private suspend fun inlineValue(
        eval: EvalOnDartLibrary,
        vm: VmService,
        ref: InstanceRef?
    ): String {
        if (ref == null) return "null"
        val instance = try {
            eval.getInstance(vm.getMainIsolateId(), ref)
        } catch (e: Exception) {
            return "<expired>"
        }
        if (instance.isNull()) return "null"
        return when (instance.getKind()) {
            InstanceKind.String -> "\"${instance.getValueAsString() ?: ""}\""
            InstanceKind.Int,
            InstanceKind.Double -> instance.getValueAsString() ?: "0"
            InstanceKind.Bool -> instance.getValueAsString() ?: "false"
            InstanceKind.Type -> instance.getName()
            else -> {
                val typeName = instance.getClassRef().getName().ifBlank { "Object" }
                val hash = runCatching { instance.getIdentityHashCode() }.getOrNull()
                if (hash != null && hash != 0) "$typeName #${hash.toString(16).take(6)}" else typeName
            }
        }
    }
}
