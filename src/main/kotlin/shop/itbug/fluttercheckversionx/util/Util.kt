package shop.itbug.fluttercheckversionx.util

import com.google.common.base.CaseFormat
import shop.itbug.fluttercheckversionx.constance.dartKeys
import java.awt.Color
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


fun Color.toHexString(): String {
    return Util.toHexFromColor(this)
}

class Util {
    companion object {


        /**
         * Color对象转换成字符串
         * @param color Color对象
         * @return 16进制颜色字符串
         */
         fun toHexFromColor(color: Color): String {
            val su = StringBuilder()
            var r: String = Integer.toHexString(color.red)
            var g: String = Integer.toHexString(color.green)
            var b: String = Integer.toHexString(color.blue)
            r = if (r.length == 1) "0$r" else r
            g = if (g.length == 1) "0$g" else g
            b = if (b.length == 1) "0$b" else b
            r = r.uppercase(Locale.getDefault())
            g = g.uppercase(Locale.getDefault())
            b = b.uppercase(Locale.getDefault())
            su.append(r)
            su.append(g)
            su.append(b)
            return su.toString()
        }

        /**
         * 字符串转换成Color对象
         * @param colorStr 16进制颜色字符串
         * @return Color对象
         */
        fun toColorFromString(colorStr: String): Color {
            var colorStr = colorStr
            colorStr = colorStr.substring(4)
            //java.awt.Color[r=0,g=0,b=255]
            return Color(colorStr.toInt(16))
        }


        fun removeSpecialCharacters(string: String): String {
            var str1: String = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, string)
            str1 = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str1)
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
                val `is`: Enumeration<InetAddress> = n.getInetAddresses()
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


    }
}