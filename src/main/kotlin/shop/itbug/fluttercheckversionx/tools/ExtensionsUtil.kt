package shop.itbug.fluttercheckversionx.tools

import kotlinx.coroutines.*


/**
 * 闭包使用：launchIO({异步},{挂起函数同步主线程},{异常返回，可以省略})
 *
 * @param block 异步协程，{可以实现delay()、repeat()、async()、await()、suspend()等}
 * @param callback 同步主协程，view更新
 * @param error 异常返回
 * @return 可操作协程 job.cancelAndJoin()
 */
@OptIn(DelicateCoroutinesApi::class)
fun <T> launchIOToMain(
    block:  suspend CoroutineScope.() -> T,
    callback:(T) -> Unit,
    error: ((Exception) -> Unit) = {}
): Job {
    return GlobalScope.launch {
        try {
            val data = withContext(Dispatchers.IO) { //协程切换，得到IO协程的泛型结果
                block()
            }
            withContext(Dispatchers.Main) {//协程切换主协程
                callback(data)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {//异常
                error.invoke(e)
            }
        }
    }
}
