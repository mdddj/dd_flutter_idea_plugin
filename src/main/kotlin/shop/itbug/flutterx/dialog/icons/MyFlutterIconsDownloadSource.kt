package shop.itbug.flutterx.dialog.icons

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import shop.itbug.flutterx.dialog.DownloadDescription
import shop.itbug.flutterx.dialog.DownloadSource
import shop.itbug.flutterx.util.PluginDescUtil
import java.io.File

data class MaterialIconItem(
    val type: String,
    @SerializedName("font_family")
    val fontFamily: String,
    val filename: String,
    val title: String,
    val icons: List<FlutterIcon>
) {
    @Transient
    private var _textFontFamily: FontFamily? = null
    val textFontFamily: FontFamily?
        get() {
            if (_textFontFamily == null) {
                val file = File(PluginDescUtil.getPluginFontsDir() + File.separator + filename)
                if (file.exists()) {
                    _textFontFamily = FontFamily(Font(identity = fontFamily, data = file.readBytes()))
                }
            }
            return _textFontFamily
        }
}

data class FlutterIcon(
    @SerializedName("icon_name") val name: String,
    @SerializedName("icon_code") val code: String,
    @Transient
    var fontFamily: FontFamily
) {
    val char: Char get() = code.removePrefix("0x").toInt(16).toChar()
}

interface FlutterIconBase {
    val icons: Lazy<List<FlutterIcon>>
}

enum class MaterialIconsType(val filename: String, val size: String) {
    Fill("MaterialIcons-Regular.ttf", "348 KB"),
    Outlined("MaterialIconsOutlined-Regular.otf", "331 KB"),
    Round("MaterialIconsRound-Regular.otf", "391 KB"),
    Sharp("MaterialIconsSharp-Regular.otf", "279 KB"),
    TwoTone("MaterialIconsTwoTone-Regular.otf", "660 KB")

}

object MyFlutterIconsDownloadSource {

    val cupertinoTTF = MyDownloadCupertinoTtfSource()
    val cupertinoIconJson = MyDownloadCupertinoFontJsonSource()
    val materialJson = MaterialDownloadFontJsonSource()
    val materialDefaultImpl = MaterialIconImplByType()

    //所有类别
    val materialAllSource = listOf(
        *MaterialIconsType.entries.map { MaterialDownloadFontSource(it.filename, it.size) }.toTypedArray(),
        materialJson
    )

    fun parseIcons(source: DownloadSource): List<FlutterIcon> {
        val buff = source.getFileBuffReader() ?: return emptyList()
        val iconListType = object : TypeToken<List<FlutterIcon>>() {}.type
        return Gson().fromJson(buff, iconListType)
    }

    fun parseMaterialIcons(source: DownloadSource): List<MaterialIconItem> {
        val buff = source.getFileBuffReader() ?: return emptyList()
        val iconListType = object : TypeToken<List<MaterialIconItem>>() {}.type
        return Gson().fromJson(buff, iconListType)
    }

    fun cupertinoIcon() = CupertinoIconImpl()
}


class MaterialIconImplByType(val type: MaterialIconsType? = null) : FlutterIconBase {
    private val items: List<MaterialIconItem> by lazy {
        MyFlutterIconsDownloadSource.parseMaterialIcons(MyFlutterIconsDownloadSource.materialJson)
    }
    override val icons: Lazy<List<FlutterIcon>>
        get() = lazy {

            if (type != null) {
                val findItem = items.find { it.filename == type.filename }
                val iconList = findItem?.icons ?: emptyList()
                if (findItem != null) {
                    iconList.forEach { ic ->
                        findItem.textFontFamily?.let {
                            ic.fontFamily = it
                        }

                    }
                }
                iconList
            } else {
                val result = mutableListOf<FlutterIcon>()
                items.forEach { itemModel ->
                    val iconList = itemModel.icons
                    iconList.forEach { ic ->
                        itemModel.textFontFamily?.let {
                            ic.fontFamily = it
                        }
                        result.add(ic)

                    }
                }
                result
            }

        }

}


class CupertinoIconImpl : FlutterIconBase {
    private val fontFamily by lazy {
        FontFamily(
            Font(
                identity = "CupertinoIcons",
                data = MyFlutterIconsDownloadSource.cupertinoTTF.getFile().readBytes()
            )
        )
    }
    override val icons: Lazy<List<FlutterIcon>>
        get() = lazy {
            val list = MyFlutterIconsDownloadSource.parseIcons(MyFlutterIconsDownloadSource.cupertinoIconJson)
            list.forEach {
                it.fontFamily = fontFamily
            }
            list
        }
}


// cupertino ttf
class MyDownloadCupertinoTtfSource() : DownloadSource() {
    override val url: String
        get() = "https://github.com/mdddj/get_flutter_cupertino_icons/raw/refs/heads/main/CupertinoIcons.ttf"

    override fun getSaveToFile(): String {
        return PluginDescUtil.getPluginFontsDir() + File.separator + "CupertinoIcons.ttf"
    }

    override val description: DownloadDescription
        get() = DownloadDescription(
            "CupertinoIcons.ttf",
            "252 KB"
        )
}

// cupertino json映射
class MyDownloadCupertinoFontJsonSource() : DownloadSource() {
    override val url: String
        get() = "https://raw.githubusercontent.com/mdddj/get_flutter_cupertino_icons/refs/heads/main/cupertino_icons.json"

    override fun getSaveToFile(): String {
        return PluginDescUtil.getPluginFontsDir() + File.separator + "cupertino_icons.json"
    }

    override val description: DownloadDescription
        get() = DownloadDescription(
            "cupertino_icons.json",
            "68.8 KB"
        )
}

class MaterialDownloadFontSource(
    val fileName: String,
    val fontSize: String,
) : DownloadSource() {
    override val url: String
        get() = "https://raw.githubusercontent.com/mdddj/parse_material_icons/refs/heads/main/material-design-icons-font/${fileName}"

    override fun getSaveToFile(): String {
        return PluginDescUtil.getPluginFontsDir() + File.separator + fileName
    }

    override val description: DownloadDescription
        get() = DownloadDescription(fileName, fontSize)

}

//下载和解析 json文件
class MaterialDownloadFontJsonSource(
) : DownloadSource() {
    override val url: String
        get() = "https://raw.githubusercontent.com/mdddj/parse_material_icons/refs/heads/main/material_icons.json"

    override fun getSaveToFile(): String {
        return PluginDescUtil.getPluginFontsDir() + File.separator + "material_icons.json"
    }

    override val description: DownloadDescription
        get() = DownloadDescription("material_icons.json", "408 kb")

}