package shop.itbug.fluttercheckversionx.util

import com.google.common.base.CaseFormat
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import shop.itbug.fluttercheckversionx.constance.dartKeys
import shop.itbug.fluttercheckversionx.manager.DartFactoryConstructorDeclarationImplManager
import java.awt.Color
import java.awt.Point
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


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

///多行的情况
fun PsiElement.getMultiLineRelativePoint(editor: Editor): RelativePoint {
    val textRange = this.textRange
    val startVisual = editor.offsetToVisualPosition(textRange.startOffset)
    val endVisual = editor.offsetToVisualPosition(textRange.endOffset)

    // 计算中间行位置
    val midLine = (startVisual.line + endVisual.line) / 2
    val midVisual = VisualPosition(midLine, 0)

    // 获取该行中间列的 X 坐标
    val midPoint = editor.visualPositionToXY(midVisual)
    val lineEnd = editor.visualPositionToXY(VisualPosition(midLine, Int.MAX_VALUE))

    // 计算行水平中点
    val x = (midPoint.x + lineEnd.x) / 2

    // 垂直中点
    val y = midPoint.y + editor.lineHeight / 2

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
            val addrs: MutableSet<InetAddress> = HashSet<InetAddress>()
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
                    ) addrs.add(i)
                }
            }
            return addrs
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
         * @param str 待校验字符串
         * @return true 包含中文字符 false 不包含中文字符
         */
        fun isContainChinese(str: String): Boolean {
            val p: Pattern = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]")
            val m: Matcher = p.matcher(str)
            return m.find()
        }


    }
}


fun getJsonString(any: Any): String {
    return GsonBuilder().setPrettyPrinting().create().toJson(any)
}