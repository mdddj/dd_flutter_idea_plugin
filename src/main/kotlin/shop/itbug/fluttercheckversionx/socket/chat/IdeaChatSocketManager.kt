package shop.itbug.fluttercheckversionx.socket.chat

import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket


object IdeaChatSocketManager {

    private val opts = IO.Options().apply {
        path = "/idea-chat"
        reconnection = false
    }


    fun connect() {
        println("开始连接socket")
        val webSocket = IO.socket("http://127.0.0.1", opts)
        webSocket.connect().on(Socket.EVENT_CONNECT) {
            println("连接成功...")
        }.on(Socket.EVENT_CONNECT_ERROR) {
            println("连接失败:${it.toString()}")
        }

    }
}