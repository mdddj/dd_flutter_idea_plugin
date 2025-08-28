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
package vm.element

// 此文件由脚本生成：dart-lang/sdk 中的 pkg/vm_service/tool/generate.dart

/**
 * 向 {@link EventKind} 添加新值被认为是向后兼容的更改。客户端应忽略无法识别的事件。
 */
enum class EventKind {
    /**
     * 已为隔离区添加断点。
     */
    BreakpointAdded,

    /**
     * 断点已被移除。
     */
    BreakpointRemoved,

    /**
     * 隔离区的未解析断点已被解析。
     */
    BreakpointResolved,

    /**
     * 断点已被更新。
     */
    BreakpointUpdated,

    /**
     * 最近收集的 CPU 采样块。
     */
    CpuSamples,

    /**
     * 来自 dart:developer.postEvent 的事件。
     */
    Extension,

    /**
     * 垃圾回收事件。
     */
    GC,

    /**
     * 来自 dart:developer.inspect 的通知。
     */
    Inspect,

    /**
     * 隔离区已退出的通知。
     */
    IsolateExit,

    /**
     * 隔离区已重新加载的通知。
     */
    IsolateReload,

    /**
     * 隔离区准备运行的通知。
     */
    IsolateRunnable,

    /**
     * 新隔离区已启动的通知。
     */
    IsolateStart,

    /**
     * 隔离区标识信息已更改的通知。目前用于通知通过 setName 更改隔离区调试名称。
     */
    IsolateUpdate,

    /**
     * 来自 dart:developer.log 的事件。
     */
    Logging,

    /**
     * 表示隔离区尚未可运行。仅出现在隔离区的 pauseEvent 中。永远不会通过流发送。
     */
    None,

    /**
     * 隔离区在断点处暂停或由于单步执行而暂停。
     */
    PauseBreakpoint,

    /**
     * 隔离区由于异常而暂停。
     */
    PauseException,

    /**
     * 隔离区在退出时暂停，在终止之前。
     */
    PauseExit,

    /**
     * 隔离区由于通过暂停中断而暂停。
     */
    PauseInterrupted,

    /**
     * 隔离区在服务请求后暂停。
     */
    PausePostRequest,

    /**
     * 隔离区在启动时暂停，在执行代码之前。
     */
    PauseStart,

    /**
     * 隔离区已开始或恢复执行。
     */
    Resume,

    /**
     * 扩展 RPC 已在隔离区上注册的通知。
     */
    ServiceExtensionAdded,

    /**
     * 服务已从另一个客户端注册到服务协议中的通知。
     */
    ServiceRegistered,

    /**
     * 服务已从另一个客户端从服务协议中移除的通知。
     */
    ServiceUnregistered,

    /**
     * 时间线事件块已完成。
     *
     * 此服务事件不会为单个时间线事件发送。它受缓冲影响，因此如果没有后续时间线事件来完成块，
     * 最新的时间线事件可能永远不会包含在任何 TimelineEvents 事件中。
     */
    TimelineEvents,

    /**
     * 活动时间线流集合已通过 `setVMTimelineFlags` 更改。
     */
    TimelineStreamSubscriptionsUpdate,

    /**
     * 隔离区的 UserTag 已更改的通知。
     */
    UserTagChanged,

    /**
     * VM 标志已通过服务协议更改的通知。
     */
    VMFlagUpdate,

    /**
     * VM 标识信息已更改的通知。目前用于通知通过 setVMName 更改 VM 调试名称。
     */
    VMUpdate,

    /**
     * 字节写入的通知，例如写入 stdout/stderr。
     */
    WriteEvent,

    /**
     * 表示 VM 返回但此客户端未知的值。
     */
    Unknown,

    ToolEvent
}
