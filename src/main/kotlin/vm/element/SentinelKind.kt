package vm.element

/**
 * [SentinelKind] 用于区分不同类型的 [Sentinel] 对象。
 */
@Suppress("unused")
enum class SentinelKind {
    /**
     * 表示变量或字段正在初始化过程中。
     */
    BeingInitialized,

    /**
     * 表示所引用的对象已被 GC 收集。
     */
    Collected,

    /**
     * 表示对象 ID 已过期。
     */
    Expired,

    /**
     * 保留以供将来使用。
     */
    Free,

    /**
     * 表示变量或字段尚未初始化。
     */
    NotInitialized,

    /**
     * 表示变量已被优化编译器消除。
     */
    OptimizedOut,

    /**
     * 表示由 VM 返回但此客户端未知的值。
     */
    Unknown
}
