/*
 * Copyright (c) 2017, the Dart project authors.
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
package vm

import com.google.gson.JsonObject

interface RemoteServiceCompleter {
    /**
     * 当服务请求成功完成时应调用此方法。
     *
     * @param result 请求的结果
     */
    fun result(result: JsonObject)

    /**
     * 当服务请求因错误而完成时应调用此方法。
     *
     * @param code    请求生成的错误代码
     * @param message 错误的描述
     * @param data    [可选] 错误的描述
     */
    fun error(code: Int, message: String, data: JsonObject?)
}
