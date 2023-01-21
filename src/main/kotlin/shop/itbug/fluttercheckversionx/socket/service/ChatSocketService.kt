package shop.itbug.fluttercheckversionx.socket.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompConnectionException
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.util.CredentialUtil
import java.time.Duration


/**
 * 连接socket单例对象
 */

object ChatSocketService {


    private lateinit var stompClient: StompClient
    private var session : StompSession? = null
    /**
     * 连接socket
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun connect() {
        CredentialUtil.token?.let { token ->
            GlobalScope.launch(Dispatchers.IO) {
                val okHttpClient = OkHttpClient.Builder()
                    .callTimeout(Duration.ofMinutes(1))
                    .pingInterval(Duration.ofSeconds(10))
                    .build()
                val wsClient = OkHttpWebSocketClient(okHttpClient)
                stompClient = StompClient(wsClient)
                try {
                    println("正在连接socket..")
                    session =  stompClient.connect(
                        "ws://${SERVICE.host}/idea-chat?token=$token",
                        customStompConnectHeaders = mapOf("Authorization" to token)
                    )
                    val subscribeText = session!!.subscribeText("/topic")
                    launch {
                        subscribeText.collect {

                        }
                    }
                    println("连接socket服务成功✅")
                } catch (e: StompConnectionException) {
                    println("Error: 连接失败:$e")
                }
            }
        }

    }


}