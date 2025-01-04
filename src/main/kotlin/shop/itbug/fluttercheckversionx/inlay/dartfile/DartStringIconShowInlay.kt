package shop.itbug.fluttercheckversionx.inlay.dartfile

import java.io.File

/**
 * 检测dart文件中的字符串,或者字符串引用,来显示一个图片
 */
class DartStringIconShowInlay {
    data class FileResult(
        val file: File, val basePath: String, val full: String
    )
}