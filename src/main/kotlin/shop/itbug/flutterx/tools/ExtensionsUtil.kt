package shop.itbug.flutterx.tools

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.BorderFactory
import javax.swing.border.Border


fun emptyBorder(): Border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
fun Project.flutterLibFolder(): VirtualFile? = guessProjectDir()?.findChild("lib")

inline fun <reified T : Any> T.log() = logger<T>()