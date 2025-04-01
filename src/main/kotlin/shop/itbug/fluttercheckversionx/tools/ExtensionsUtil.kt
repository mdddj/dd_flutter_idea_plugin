package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.diagnostic.logger
import javax.swing.BorderFactory
import javax.swing.border.Border


fun emptyBorder(): Border = BorderFactory.createEmptyBorder(0, 0, 0, 0)


inline fun <reified T : Any> T.log() = logger<T>()