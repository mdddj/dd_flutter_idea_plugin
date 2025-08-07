package vm.consumer

import vm.element.Success

@Suppress("unused")
interface SuccessConsumer : Consumer {
    fun received(response: Success)
}
