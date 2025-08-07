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
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import vm.logging.Logging

/**
 * A Ktor WebSocketSession based implementation of [RequestSink].
 *
 * @param session The active WebSocket session to send messages to.
 * @param scope The CoroutineScope to launch send/close operations.
 */
class WebSocketRequestSink(
    private var session: WebSocketSession?,
    private val scope: CoroutineScope
) : RequestSink {

    override fun add(json: JsonObject) {
        val currentSession = session
        val request = json.toString()

        // If the session is null (already closed), log and drop the message.
        if (currentSession == null) {
            Logging.getLogger().logInformation("Dropped (session closed): $request")
            return
        }

        // Launch a new coroutine in the provided scope to send the message.
        // This prevents blocking the caller thread.
        scope.launch {
            Logging.getLogger().logInformation("Sent: $request")
            try {
                currentSession.send(Frame.Text(request))
            } catch (e: Exception) {
                // Handle exceptions, e.g., if the connection is closed while trying to send.
                Logging.getLogger().logError("Failed to send request: $request", e)
            }
        }
    }

    override fun close() {
        val sessionToClose = session
        // Set session to null immediately to prevent new messages from being sent.
        session = null

        if (sessionToClose != null) {
            // Launch a coroutine to close the session asynchronously.
            scope.launch {
                try {
                    sessionToClose.close(CloseReason(CloseReason.Codes.NORMAL, "Client closed connection"))
                } catch (e: Exception) {
                    Logging.getLogger().logError("Failed to close websocket", e)
                }
            }
        }
    }
}