/*
 * Copyright (c) 2015, the Dart project authors.
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

import vm.element.Event


/**
 * 由 {@link VmService} 使用的接口，用于通知其他对象 VM 事件。
 */
interface VmServiceListener {
    fun connectionOpened()

    /**
     * 当接收到 VM 事件时调用。
     *
     * @param streamId 流标识符（例如 {@link VmService#DEBUG_STREAM_ID}）
     * @param event    事件
     */
    fun received(streamId: String, event: Event)

    fun connectionClosed()
}
