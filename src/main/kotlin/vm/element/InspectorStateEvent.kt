package vm.element

/**
 * Inspector状态变化事件
 */
data class InspectorStateEvent(
    val overlayEnabled: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)