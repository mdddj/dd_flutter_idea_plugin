/*
 * Copyright (c) 2014, the Dart project authors.
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

/**
 * {@code Logging} provides a global instance of {@link Logger}.
 */
class Logging {
    companion object {
        private var logger: Logger = Logger.IDEA

        @JvmStatic
        fun getLogger(): Logger {
            return logger
        }

        @JvmStatic
        fun setLogger(logger: Logger?) {
            this.logger = logger ?: Logger.CONSOLE
        }
    }
}
