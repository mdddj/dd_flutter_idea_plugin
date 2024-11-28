package shop.itbug.fluttercheckversionx.util

import shop.itbug.fluttercheckversionx.services.PluginStateService
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object DateUtils {
    fun timeAgo(
        timeString: String,
        format: String = "yyyy-MM-dd HH:mm:ss",
    ): String {
        if (timeString.isEmpty()) return ""
        try {
            val formatter = DateTimeFormatter.ofPattern(format)
            val pastTime = LocalDateTime.parse(timeString, formatter)
            val pastInstant = pastTime.atZone(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()
            val duration = Duration.between(pastInstant, now)

            var text = when {
                duration.toMinutes() < 1 -> "seconds ago"
                duration.toHours() < 1 -> "minutes ago"
                duration.toDays() < 1 -> "hours ago"
                duration.toDays() < 30 -> "days ago"
                duration.toDays() < 365 -> "months ago"
                else -> "years ago"
            }.trim()
            text = localizeTimeAgo(text, PluginStateService.getInstance().state?.getSettingLocale() ?: Locale.US)
            val time = when {
                duration.toMinutes() < 1 -> "${duration.seconds}"
                duration.toHours() < 1 -> "${duration.toMinutes()}"
                duration.toDays() < 1 -> "${duration.toHours()}"
                duration.toDays() < 30 -> "${duration.toDays()}"
                duration.toDays() < 365 -> "${duration.toDays() / 30}"
                else -> "${duration.toDays() / 365}"
            }
            return "$time$text"
        } catch (e: Exception) {
            e.printStackTrace()
            return timeString
        }
    }


    fun parseDate(timeString: String): String {
        if (timeString.isBlank()) return ""
        var date = timeString
        val toCharArray = date.toCharArray()
        val tChat = toCharArray[10]
        if (tChat == 'T') {
            date = date.replace("T", " ")
        }
        val dotIndex = date.lastIndexOf(".")
        date = date.substring(0, dotIndex)
        return date
    }

    private fun localizeTimeAgo(text: String, locale: Locale): String {
        return when (locale.language) {
            "en" -> text
            "es" -> translateToSpanish(text)
            "zh" -> translateToChinese(text, locale)
            "ko" -> translateToKorean(text)
            "ja" -> translateToJapanese(text)
            "zh_TW" -> translateToTraditionalChinese(text)
            else -> text
        }
    }

    private fun translateToSpanish(text: String): String {
        return when (text) {
            "seconds ago" -> "hace segundos"
            "minutes ago" -> "hace minutos"
            "hours ago" -> "hace horas"
            "days ago" -> "hace días"
            "months ago" -> "hace meses"
            "years ago" -> "hace años"
            else -> text
        }
    }

    private fun translateToChinese(text: String, locale: Locale): String {
        return when (text) {
            "seconds ago" -> if (locale.country == "CN") "秒前" else "秒前" // Simplified and Traditional are the same in this case
            "minutes ago" -> if (locale.country == "CN") "分钟前" else "分鐘前"
            "hours ago" -> if (locale.country == "CN") "小时前" else "小時前"
            "days ago" -> if (locale.country == "CN") "天前" else "天前"
            "months ago" -> if (locale.country == "CN") "个月前" else "月前"
            "years ago" -> if (locale.country == "CN") "年前" else "年前"
            else -> text
        }
    }

    private fun translateToKorean(text: String): String {
        return when (text) {
            "seconds ago" -> "초 전"
            "minutes ago" -> "분 전"
            "hours ago" -> "시간 전"
            "days ago" -> "일 전"
            "months ago" -> "개월 전"
            "years ago" -> "년 전"
            else -> text
        }
    }

    private fun translateToJapanese(text: String): String {
        return when (text) {
            "seconds ago" -> "秒前"
            "minutes ago" -> "分前"
            "hours ago" -> "時間前"
            "days ago" -> "日前"
            "months ago" -> "ヶ月前"
            "years ago" -> "年前"
            else -> text
        }
    }

    private fun translateToTraditionalChinese(text: String): String {
        return when (text) {
            "seconds ago" -> "秒前"
            "minutes ago" -> "分鐘前"
            "hours ago" -> "小時前"
            "days ago" -> "天前"
            "months ago" -> "個月前"
            "years ago" -> "年前"
            else -> text
        }
    }

}