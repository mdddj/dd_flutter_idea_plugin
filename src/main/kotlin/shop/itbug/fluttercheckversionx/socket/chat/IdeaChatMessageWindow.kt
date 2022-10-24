package shop.itbug.fluttercheckversionx.socket.chat
//idea 聊天窗口的socket服务
class IdeaChatMessageWindow : Runnable {

    override fun run() {
       IdeaChatSocketManager.connect()
    }
}