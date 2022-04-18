package socket

import cn.hutool.core.io.IoUtil
import cn.hutool.core.lang.Console
import cn.hutool.core.util.StrUtil
import cn.hutool.socket.nio.NioServer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer


// socket 连接flutter项目服务类
class ProjectSocketService {

    /// 项目打开,开启一个socket服务,进行接口监听传输
    @OptIn(DelicateCoroutinesApi::class)
    fun onOpen(){
        GlobalScope.launch {
             launch {
                 val server = NioServer(6667)
                 server.setChannelHandler { sc ->
                     val readBuffer: ByteBuffer = ByteBuffer.allocate(1024)
                     try {
                         val readBytes: Int = sc.read(readBuffer)
                         if (readBytes > 0) {
                             readBuffer.flip()
                             val bytes = ByteArray(readBuffer.remaining())
                             readBuffer.get(bytes)
                             val body: String = StrUtil.utf8Str(bytes)
                             Console.log("-->[{}]: {}", sc.remoteAddress, body)
                         } else if (readBytes < 0) {
                             IoUtil.close(sc)
                         }
                     } catch (e: IOException) {
                         println("出现错误:${e.localizedMessage}")
                     }
                 }
                 server.listen()
             }
        }
    }
}