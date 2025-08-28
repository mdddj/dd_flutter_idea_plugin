package vm.consumer

import vm.element.RPCError
import vm.element.Success

@Suppress("unused")
interface SuccessConsumer : Consumer {
    fun received(response: Success)
}
// 顶层函数，创建SuccessConsumer实例
fun SuccessConsumer(handler: (Success) -> Unit): SuccessConsumer = object : SuccessConsumer {
    override fun received(response: Success) {
        handler(response)
    }

    override fun onError(error: RPCError) {

    }
}