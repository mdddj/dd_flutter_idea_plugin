/*
 * Copyright (c) 2012, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package vm.logging

import com.intellij.openapi.diagnostic.thisLogger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * The interface {@code Logger} defines the behavior of objects that can be used to receive
 * information about errors. Implementations usually write this information to a file, but can also
 * record the information for later use (such as during testing) or even ignore the information.
 */
interface Logger {

    /**
     * 控制台打印日志
     */

    class ConsoleLogger : Logger {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        // ANSI 颜色代码
        companion object {
            const val RESET = "\u001B[0m"
            const val RED = "\u001B[31m"      // 红色 - 用于 ERROR
            const val GREEN = "\u001B[32m"    // 绿色 - 用于 INFO
            const val YELLOW = "\u001B[33m"   // 黄色 - 可选用于 WARN
            const val BLUE = "\u001B[34m"     // 蓝色
            const val PURPLE = "\u001B[35m"   // 紫色
            const val CYAN = "\u001B[36m"     // 青色
            const val WHITE = "\u001B[37m"    // 白色

            // 高亮颜色（更鲜艳）
            const val BRIGHT_RED = "\u001B[91m"
            const val BRIGHT_GREEN = "\u001B[92m"
            const val BRIGHT_YELLOW = "\u001B[93m"
            const val BRIGHT_BLUE = "\u001B[94m"
        }

        private fun formatLog(level: String, message: String, color: String): String {
            val time = LocalDateTime.now().format(formatter)
            return "$color[$time] [$level] $message$RESET"
        }

        override fun logError(message: String) {
            println(formatLog("ERROR", message, RED))
        }

        override fun logError(message: String, exception: Throwable) {
            println(formatLog("ERROR", "$message , exception:$exception", RED))
        }

        override fun logInformation(message: String) {
            println(formatLog("INFO", message, GREEN))
        }

        override fun logInformation(message: String, exception: Throwable) {
            println(formatLog("INFO", "$message  exception:$exception", GREEN))
        }
    }

    class IdeaLogger : Logger {
        val ideaLog = thisLogger()
        override fun logError(message: String) {
            ideaLog.error(message)
        }

        override fun logError(message: String, exception: Throwable) {
            ideaLog.error(message, exception)
        }

        override fun logInformation(message: String) {
            ideaLog.info(message)
        }

        override fun logInformation(message: String, exception: Throwable) {
            ideaLog.info(message, exception)
        }
    }

    companion object {
        @JvmField
        val CONSOLE: Logger = ConsoleLogger()

        @JvmField
        val IDEA: Logger = IdeaLogger()
    }

    /**
     * Log the given message as an error.
     *
     * @param message an explanation of why the error occurred or what it means
     */
    fun logError(message: String)

    /**
     * Log the given exception as one representing an error.
     *
     * @param message   an explanation of why the error occurred or what it means
     * @param exception the exception being logged
     */
    fun logError(message: String, exception: Throwable)

    /**
     * Log the given informational message.
     *
     * @param message an explanation of why the error occurred or what it means
     */
    fun logInformation(message: String)

    /**
     * Log the given exception as one representing an informational message.
     *
     * @param message   an explanation of why the error occurred or what it means
     * @param exception the exception being logged
     */
    fun logInformation(message: String, exception: Throwable)
}
