package vm.internal

import com.google.gson.JsonObject
import vm.logging.Logging

/**
 * A {@link RequestSink} that reports with an error to each request.
 */
class ErrorRequestSink(private val responseSink: ResponseSink, private val code: String, private val message: String) :
    RequestSink {

    override fun add(request: JsonObject) {
        val id = request.getAsJsonPrimitive(VmServiceConst.ID).asString
        try {
            // TODO(danrubel) is this the correct format for an error response?
            val error = JsonObject()
            error.addProperty(VmServiceConst.CODE, code)
            error.addProperty(VmServiceConst.MESSAGE, message)
            val response = JsonObject()
            response.addProperty(VmServiceConst.ID, id)
            response.add(VmServiceConst.ERROR, error)
            responseSink.add(response)
        } catch (e: Throwable) {
            e.message?.let { Logging.getLogger().logError(it, e) }
        }
    }

    override fun close() {}
}
