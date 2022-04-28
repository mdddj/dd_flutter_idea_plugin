package shop.itbug.fluttercheckversionx.socket

import org.smartboot.socket.Protocol
import org.smartboot.socket.transport.AioSession
import java.nio.ByteBuffer

class StringProtocol : Protocol<String?> {

    override fun decode(readBuffer: ByteBuffer, session: AioSession?): String? {
        val remaining: Int = readBuffer.remaining()
        if (remaining < Integer.BYTES) {
            return null
        }
        readBuffer.mark()
        val length: Int = readBuffer.int
        if (length > readBuffer.remaining()) {
            readBuffer.reset()
            return null
        }
        val b = ByteArray(length)
        readBuffer.get(b)
        readBuffer.mark()
        val string = String(b)
        return string
    }
}