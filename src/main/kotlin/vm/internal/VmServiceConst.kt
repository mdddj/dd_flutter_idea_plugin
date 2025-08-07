package vm.internal

/**
 * JSON constants used when communicating with the VM observatory service.
 */
interface VmServiceConst {
    companion object {
        const val CODE = "code"
        const val ERROR = "error"
        const val EVENT = "event"
        const val ID = "id"
        const val MESSAGE = "message"
        const val METHOD = "method"
        const val PARAMS = "params"
        const val RESULT = "result"
        const val STREAM_ID = "streamId"
        const val TYPE = "type"
        const val JSONRPC = "jsonrpc"
        const val JSONRPC_VERSION = "2.0"
        const val DATA = "data"

        /**
         * Parse error	Invalid JSON was received by the server.
         * An error occurred on the server while parsing the JSON text.
         */
        const val PARSE_ERROR = -32700

        /**
         * Invalid Request	The JSON sent is not a valid Request object.
         */
        const val INVALID_REQUEST = -32600

        /**
         * Method not found	The method does not exist / is not available.
         */
        const val METHOD_NOT_FOUND = -32601

        /**
         * Invalid params	Invalid method parameter(s).
         */
        const val INVALID_PARAMS = -32602

        /**
         * Server error	Reserved for implementation-defined server-errors.
         * -32000 to -32099
         */
        const val SERVER_ERROR = -32000
    }
}
