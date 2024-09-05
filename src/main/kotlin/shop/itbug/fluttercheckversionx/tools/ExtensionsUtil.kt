package shop.itbug.fluttercheckversionx.tools

import javax.swing.BorderFactory
import javax.swing.border.Border


fun emptyBorder(): Border = BorderFactory.createEmptyBorder(0, 0, 0, 0)


/**
 * 统计函数执行时间,返回毫秒
 */
fun measureExecutionTime(block: () -> Unit): Long {
    val startTime = System.currentTimeMillis()  // 获取起始时间
    block()                            // 执行传入的代码块
    val endTime = System.currentTimeMillis()  // 获取结束时间
    return endTime - startTime         // 返回执行时间（纳秒）
}
