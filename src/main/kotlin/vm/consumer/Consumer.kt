package vm.consumer

import vm.element.RPCError


/**
 * Consumer 是所有消费者接口的公共接口。
 */
interface Consumer {
    /**
     * 如果请求因某种原因失败，则调用此方法。
     */
    fun onError(error: RPCError)
}
