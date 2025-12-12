package shop.itbug.flutterx.util

import com.google.common.base.CaseFormat
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import shop.itbug.flutterx.constance.dartKeys
import shop.itbug.flutterx.manager.DartFactoryConstructorDeclarationImplManager
import java.awt.Color
import java.awt.Point
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


fun Color.toHexString(): String {
    return Util.toHexFromColor(this)
}

fun String.formatDartName(): String {
    return Util.removeSpecialCharacters(this)
}


fun String.firstChatToUpper(): String {
    return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, this)
}


/**
 * 格式化文档
 */
fun PsiElement.reformatText() {
    WriteCommandAction.runWriteCommandAction(this.project) {
        CodeStyleManager.getInstance(this.project).reformatText(this.containingFile, 0, this.containingFile.textLength)
    }
}


fun PsiElement.runWriteCommandAction(runnable: Runnable) {
    WriteCommandAction.runWriteCommandAction(project, runnable)
}

/**
 * 根据文本查找psi节点
 * @param findText 要查找的文本
 */
fun PsiFile.findPsiElementByText(findText: String): PsiElement? {
    val c = MyPsiElementUtil.findAllMatchingElements(this) { text: String, _: PsiElement ->
        return@findAllMatchingElements findText == text
    }
    return if (c.isNotEmpty()) c.first() else null
}

fun PsiElement.getRelativePoint(editor: Editor): RelativePoint {
    val startPoint = editor.offsetToXY(textRange.startOffset)
    val endPoint = editor.offsetToXY(textRange.endOffset)
    val lineHeight = editor.lineHeight

    // 计算水平中点
    val x = (startPoint.x + endPoint.x) / 2

    // 计算垂直中点：起始 Y 坐标 + 半行高
    val y = startPoint.y + lineHeight / 2

    return RelativePoint(editor.contentComponent, Point(x, y))
}

/**
 * 根据psi节点获取文件名
 */
fun PsiElement.getFileName(): String {
    return FileUtilRt.getNameWithoutExtension(this.containingFile.name)
}


fun DartFactoryConstructorDeclarationImpl.manager() = DartFactoryConstructorDeclarationImplManager(this)


class Util {
    companion object {


        /**
         * 获取flutter当前版本通道
         */
        fun getFlutterChannel(): String? {
            fun findChannelNameWithStar(lines: List<String>): Pair<Int, String>? {
                lines.forEachIndexed { index, line ->
                    if (line.contains("*")) {
                        val cleanedLine = line.substringBeforeLast(" (")
                        if (cleanedLine.contains("*")) {
                            val channelName = cleanedLine.substringAfterLast(" ", "").substringBefore("*").trim()
                            return Pair(index + 1, channelName) // 返回行号（索引+1，因为索引是从0开始的）和通道名称
                        }
                    }
                }
                return null // 如果没找到星号，则返回null
            }

            val cmd = GeneralCommandLine("flutter", "channel")
            try {
                val output = ExecUtil.execAndGetOutput(cmd)
                if (output.exitCode == 0) {
                    val stderrLines = output.stdoutLines
                    val result = findChannelNameWithStar(stderrLines)
                    if (result != null) {
                        val version = result.second
                        return version
                    }
                }
            } catch (_: ProcessNotCreatedException) {
                println("无法运行flutter命令.")
                return null
            }
            return null
        }

        fun toHexFromColor(color: Color): String {
            return UIUtil.colorToHex(color)
        }


        fun removeSpecialCharacters(string: String): String {

            var str1: String = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, string)
            str1 = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str1)
            if (dartKeys.contains(str1)) {
                str1 += "_"
            }
            return str1
        }

        /**
         * 获取本机IP列表
         */
        fun resolveLocalAddresses(): Set<InetAddress> {
            val adders = HashSet<InetAddress>()
            var ns: Enumeration<NetworkInterface>? = null
            try {
                ns = NetworkInterface.getNetworkInterfaces()
            } catch (_: SocketException) {
            }
            while (ns != null && ns.hasMoreElements()) {
                val n: NetworkInterface = ns.nextElement()
                val `is`: Enumeration<InetAddress> = n.inetAddresses
                while (`is`.hasMoreElements()) {
                    val i: InetAddress = `is`.nextElement()
                    if (!i.isLoopbackAddress && !i.isLinkLocalAddress && !i.isMulticastAddress
                        && !isSpecialIp(i.hostAddress)
                    ) adders.add(i)
                }
            }
            return adders
        }

        private fun isSpecialIp(ip: String): Boolean {
            if (ip.contains(":")) return true
            if (ip.startsWith("127.")) return true
            if (ip.startsWith("169.254.")) return true
            if (ip.split(".").size == 4 && ip.split(".")[2] === "0") return false
            return ip == "255.255.255.255"
        }

        /**
         * 字符串是否包含中文
         *
         * @param text 待校验字符串
         * @return true 包含中文字符 false 不包含中文字符
         */
        fun isContainChinese(text: String): Boolean {
            if (text.isEmpty()) {
                return false
            }

            // \p{IsHan} 是一个 Unicode 属性，用于匹配 CJK 象形文字（Han characters）。
            // 这是最精准和最全面的方式之一。
            val regex = Regex(".*\\p{IsHan}.*")

            // 或者使用 containsMatchIn 更为直接
            // val regex = Regex("\\p{IsHan}")
            // return regex.containsMatchIn(text)

            return text.matches(regex)
        }


    }
}


fun getJsonString(any: Any): String {
    return GsonBuilder().setPrettyPrinting().create().toJson(any)
}