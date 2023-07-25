package shop.itbug.fluttercheckversionx.util

import com.google.common.base.CaseFormat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
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
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


fun Color.toHexString(): String {
    return Util.toHexFromColor(this)
}

fun String.formatDartName() : String {
    return Util.removeSpecialCharacters(this)
}


fun String.firstChatToUpper() : String {
    return  CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, this)
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
    WriteCommandAction.runWriteCommandAction(project,runnable)
}

/**
 * 根据文本查找psi节点
 * @param findText 要查找的文本
 */
fun PsiFile.findPsiElementByText(findText: String) : PsiElement? {
    val c  = MyPsiElementUtil.findAllMatchingElements(this){ text: String, _: PsiElement ->
        return@findAllMatchingElements findText == text
    }
    return if(c.isNotEmpty()) c.first() else null
}

fun PsiElement.getRelativePoint(editor: Editor): RelativePoint {
    val startPoint: Point = editor.offsetToXY(textRange.startOffset)
    val endPoint: Point = editor.offsetToXY(textRange.endOffset)
    val x: Int = (startPoint.x + endPoint.x) / 2
    val y: Int = startPoint.y
    return RelativePoint(editor.contentComponent, Point(x, y))
}

/**
 * 根据psi节点获取文件名
 */
fun PsiElement.getFileName() : String {
    return FileUtilRt.getNameWithoutExtension(this.containingFile.name)
}


fun DartFactoryConstructorDeclarationImpl.manager() = DartFactoryConstructorDeclarationImplManager(this)



class Util {
    companion object {

        val userHomePath: String get() = System.getProperty("user.home")

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
            if(ip.split(".").size == 4 && ip.split(".")[2].toString() === "0") return false
            return ip == "255.255.255.255"
        }

        object RelativeDateFormat {
            private const val ONE_MINUTE = 60000L
            private const val ONE_HOUR = 3600000L
            private const val ONE_DAY = 86400000L
            private const val ONE_WEEK = 604800000L
            private const val ONE_SECOND_AGO = "秒前"
            private const val ONE_MINUTE_AGO = "分钟前"
            private const val ONE_HOUR_AGO = "小时前"
            private const val ONE_DAY_AGO = "天前"
            private const val ONE_MONTH_AGO = "月前"
            private const val ONE_YEAR_AGO = "年前"
            fun format(time: String?): String {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                var date: Date? = null
                try {
                    date = format.parse(time)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                val delta: Long = Date().time - (date!!.time)
                if (delta < 1L * ONE_MINUTE) {
                    val seconds = toSeconds(delta)
                    return (if (seconds <= 0) 1 else seconds).toString() + ONE_SECOND_AGO
                }
                if (delta < 45L * ONE_MINUTE) {
                    val minutes = toMinutes(delta)
                    return (if (minutes <= 0) 1 else minutes).toString() + ONE_MINUTE_AGO
                }
                if (delta < 24L * ONE_HOUR) {
                    val hours = toHours(delta)
                    return (if (hours <= 0) 1 else hours).toString() + ONE_HOUR_AGO
                }
                if (delta < 48L * ONE_HOUR) {
                    return "昨天"
                }
                if (delta < 30L * ONE_DAY) {
                    val days = toDays(delta)
                    return (if (days <= 0) 1 else days).toString() + ONE_DAY_AGO
                }
                return if (delta < 12L * 4L * ONE_WEEK) {
                    val months = toMonths(delta)
                    (if (months <= 0) 1 else months).toString() + ONE_MONTH_AGO
                } else {
                    val years = toYears(delta)
                    (if (years <= 0) 1 else years).toString() + ONE_YEAR_AGO
                }
            }

            private fun toSeconds(date: Long): Long {
                return date / 1000L
            }

            private fun toMinutes(date: Long): Long {
                return toSeconds(date) / 60L
            }

            private fun toHours(date: Long): Long {
                return toMinutes(date) / 60L
            }

            private fun toDays(date: Long): Long {
                return toHours(date) / 24L
            }

            private fun toMonths(date: Long): Long {
                return toDays(date) / 30L
            }

            private fun toYears(date: Long): Long {
                return toMonths(date) / 365L
            }
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



        fun addStringToLineStart(text:String, value:String) : String {
            val bufferedReader = BufferedReader(InputStreamReader(ByteArrayInputStream(text.toByteArray())))
            val sb = StringBuilder()
            bufferedReader.forEachLine {
                sb.appendLine("$value $it")
            }
            return sb.toString()
        }

    }
}