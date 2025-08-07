/*
 * Copyright (c) 2015, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package vm.internal

import com.google.gson.JsonObject
import vm.logging.Logging

/**
 * A {@link RequestSink} that reports with an error to each request.
 */
class ErrorRequestSink(private val responseSink: ResponseSink, private val code: String, private val message: String) :
    RequestSink {
    init {
        if (responseSink == null || code == null || message == null) {
            throw IllegalArgumentException("Unexpected null argument: $responseSink $code $message")
        }
    }

    override fun add(request: JsonObject) {
        val id = request.getAsJsonPrimitive(VmServiceConst.Companion.ID).asString
        try {
            // TODO(danrubel) is this the correct format for an error response?
            val error = JsonObject()
            error.addProperty(VmServiceConst.Companion.CODE, code)
            error.addProperty(VmServiceConst.Companion.MESSAGE, message)
            val response = JsonObject()
            response.addProperty(VmServiceConst.Companion.ID, id)
            response.add(VmServiceConst.Companion.ERROR, error)
            responseSink.add(response)
        } catch (e: Throwable) {
            e.message?.let { Logging.getLogger().logError(it, e) }
        }
    }

    override fun close() {}
}
