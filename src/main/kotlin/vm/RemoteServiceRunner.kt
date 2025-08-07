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

/**
 * 由 {@link VmService} 使用的接口，用于向服务注册回调。
 */
interface RemoteServiceRunner {
    /**
     * 当接收到服务请求时调用。
     *
     * @param params    请求的参数
     * @param completer 在执行结束时调用的完成器
     */
    fun run(params: JsonObject, completer: RemoteServiceCompleter)
}
