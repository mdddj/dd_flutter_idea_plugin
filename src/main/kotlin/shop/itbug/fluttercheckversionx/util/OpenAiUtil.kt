package shop.itbug.fluttercheckversionx.util

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.flow.Flow


object OpenAiUtil {

    /**
     * open ai 实例
     */
    private fun getOpenAi(): OpenAI = OpenAI(
        OpenAIConfig(
            token = CredentialUtil.openApiKey,
            timeout = Timeout()
        )
    )

    @OptIn(BetaOpenAI::class)
    fun createQ(q: String): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = listOf(
                ChatMessage(content = q, role = ChatRole.User)
            )
        )
    }


    @OptIn(BetaOpenAI::class)
    fun askSimple(q: String): Flow<ChatCompletionChunk> {
        return getOpenAi().chatCompletions(createQ(q))
    }
}