package vm.devtool

/**
 * Riverpod DevTool 的数据模型。
 *
 * 对齐官方 riverpod_devtool (packages/riverpod_devtool) 的数据结构：
 * - Frame / Event / ProviderMeta / OriginMeta / ProviderStateRef
 * - 每帧累积的 ElementMeta（状态 + 依赖关系）
 */
sealed class RiverpodEventType {
    data object ProviderContainerAdd : RiverpodEventType()
    data object ProviderContainerDispose : RiverpodEventType()
    data object ProviderElementAdd : RiverpodEventType()
    data object ProviderElementUpdate : RiverpodEventType()
    data object ProviderElementDispose : RiverpodEventType()
    data object ProviderDependencyChange : RiverpodEventType()
    data class Unknown(val typeName: String) : RiverpodEventType()

    companion object {
        fun fromString(s: String): RiverpodEventType = when (s) {
            "ProviderContainerAddEvent" -> ProviderContainerAdd
            "ProviderContainerDisposeEvent" -> ProviderContainerDispose
            "ProviderElementAddEvent" -> ProviderElementAdd
            "ProviderElementUpdateEvent" -> ProviderElementUpdate
            "ProviderElementDisposeEvent" -> ProviderElementDispose
            "ProviderDependencyChangeEvent" -> ProviderDependencyChange
            else -> Unknown(s)
        }
    }
}

fun RiverpodEventType.toDisplayString(): String = when (this) {
    RiverpodEventType.ProviderContainerAdd -> "Container Add"
    RiverpodEventType.ProviderContainerDispose -> "Container Dispose"
    RiverpodEventType.ProviderElementAdd -> "Added"
    RiverpodEventType.ProviderElementUpdate -> "Updated"
    RiverpodEventType.ProviderElementDispose -> "Disposed"
    RiverpodEventType.ProviderDependencyChange -> "Dependency Changed"
    is RiverpodEventType.Unknown -> typeName
}

/** 是否会产生/携带 state */
fun RiverpodEventType.hasStateChange(): Boolean = this is RiverpodEventType.ProviderElementAdd ||
        this is RiverpodEventType.ProviderElementUpdate

/** 是否算作 provider 生命周期变更（用于分组与帧标记） */
fun RiverpodEventType.isElementLifecycle(): Boolean = this is RiverpodEventType.ProviderElementAdd ||
        this is RiverpodEventType.ProviderElementUpdate ||
        this is RiverpodEventType.ProviderElementDispose

/**
 * Provider 的 Origin（来源）元数据，对应官方 `OriginMeta`。
 * 同一个 Origin 下的 family provider 会共享它。
 */
data class RiverpodOriginMeta(
    val id: String,
    val toStringValue: String,
    val isFamily: Boolean,
    val hashValue: String
)

/**
 * Provider 元数据，对应官方 `ProviderMeta`。
 */
data class RiverpodProviderMeta(
    val id: String,
    val origin: RiverpodOriginMeta?,
    val argToStringValue: String,
    val hashValue: String,
    val containerId: String,
    val elementId: String,
    val containerHashValue: String,
    val creationStackTrace: String?
)

/**
 * 一条 Riverpod DevTool 事件，对应官方 `Event`。
 *
 * @param statePath 求值表达式，用于获取该事件的 state 对象
 *                  （如 `RiverpodDevtool.instance.frames[5].events[2].state.state`），可为 null
 * @param notifierPath 求值表达式，用于获取该事件的 notifier state 对象，可为 null
 */
data class RiverpodEvent(
    val type: RiverpodEventType,
    val providerMeta: RiverpodProviderMeta?,
    val statePath: String? = null,
    val notifierPath: String? = null,
    /** 仅 ProviderDependencyChangeEvent：依赖变更对应的 elementId */
    val dependencyElementId: String? = null,
    /** 仅 ProviderDependencyChangeEvent：该 provider 依赖的其他 elementId */
    val dependencyParents: Set<String> = emptySet()
) {
    val hasState: Boolean get() = statePath != null
}

/**
 * 一帧，对应官方 `Frame`。
 */
data class RiverpodFrame(
    val index: Int,
    val timestamp: Long,
    val events: List<RiverpodEvent>
) {
    val hasStateChanges: Boolean get() = events.any { it.type.isElementLifecycle() }
}

/** Provider 在当前帧的状态，对应官方 `ProviderStatusInFrame` */
enum class RiverpodProviderStatus { Added, Modified, Disposed }

/**
 * 某个 ProviderElement 在某帧的累积状态，对应官方 `ElementMeta`。
 */
data class RiverpodElementState(
    val provider: RiverpodProviderMeta,
    val statePath: String?,
    val notifierPath: String?,
    val parents: Set<String>,
    val children: Set<String>
)

/**
 * Provider 列表中展示的条目（聚合了所有帧中最后一次事件的信息）。
 */
data class RiverpodProviderInfo(
    val origin: RiverpodOriginMeta?,
    val elementId: String,
    val name: String,
    val arg: String,
    val containerId: String,
    val containerHash: String,
    val hashValue: String,
    val lastEventType: RiverpodEventType,
    val status: RiverpodProviderStatus?,
    val frameIndex: Int,
    val eventIndex: Int,
    val hasState: Boolean,
    val creationStackTrace: String?
)
