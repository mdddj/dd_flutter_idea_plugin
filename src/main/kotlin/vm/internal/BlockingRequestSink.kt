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
import java.util.*

/**
 * A {@link RequestSink} that enqueues all requests and can be later converted into a "passthrough"
 * or an "error" {@link RequestSink}.
 */
class BlockingRequestSink(private val base: RequestSink) : RequestSink {
    /**
     * A queue of requests.
     */
    private val queue: LinkedList<JsonObject> = LinkedList()

    override fun add(request: JsonObject) {
        synchronized(queue) {
            queue.add(request)
        }
    }

    override fun close() {
        base.close()
    }

    /**
     * Responds with an error to all the currently queued requests and return a {@link RequestSink} to
     * do the same for all the future requests.
     *
     * @param errorResponseSink the sink to send error responses to, not {@code null}
     */
    fun toErrorSink(
        errorResponseSink: ResponseSink, errorResponseCode: String,
        errorResponseMessage: String
    ): RequestSink {
        val errorRequestSink = ErrorRequestSink(
            errorResponseSink, errorResponseCode,
            errorResponseMessage
        )
        synchronized(queue) {
            for (request in queue) {
                errorRequestSink.add(request)
            }
        }
        return errorRequestSink
    }

    /**
     * Returns the passthrough {@link RequestSink}.
     */
    fun toPassthroughSink(): RequestSink {
        synchronized(queue) {
            for (request in queue) {
                base.add(request)
            }
        }
        return base
    }
}
