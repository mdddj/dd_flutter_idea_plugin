package services

import kotlinx.coroutines.*
import model.PluginVersion

/// 携程任务管理
class WorkManager {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + job)

    ///执行扫描操作
    fun doWork(plugins: List<PluginVersion>) {
        scope.launch {

        }
    }

    ///关闭任务
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
    }
}