package shop.itbug.fluttercheckversionx.tools

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import shop.itbug.fluttercheckversionx.dialog.NameFormatRule
import shop.itbug.fluttercheckversionx.util.formatDartName

typealias VoidCallback = () -> Unit

sealed class MyDartType(var dartType: String, var defaultValue: String)
data object DartDynamicValue : MyDartType("dynamic", "")
data object DartStringValue : MyDartType("String", "''")
data object DartNumberValue : MyDartType("num", "0")
data object DartBooleanValue : MyDartType("bool", "false")
data class DartCustomObject(var className: String) :
    MyDartType(className.formatDartName(), "${className.formatDartName()}()")


enum class FormJsonType(val value: String) {
    DynamicMap("Map<String,dynamic>"), ObjectType("Map<String,Object>"), ObjectObject("Map<Object,Object>"), DynamicType(
        "dynamic"
    );

    override fun toString(): String {
        return value
    }
}

//freezed版本
enum class FreezedVersion(val version: String) {
    // 2.x版本
    DefaultVersion("2.x"),

    // 3.0版本，添加
    ThreeVersion("3.x");

    override fun toString(): String {
        return version
    }
}

// 是密封类还是抽象类
enum class FreezedClassType(var type: String) {
    Sealed("sealed"), Abstract("abstract");

    override fun toString(): String {
        return type
    }
}

data class DartArrayValue(var className: String) : MyDartType("List<${className}>", "[]")
data class MyDartProperties(var name: String, var type: MyDartType, var index: Int)
sealed class DartType
sealed class DartPropertyGenerateConfig
sealed class DartClassGenerateConfig : BaseState()
data class MyJsonObject(val name: String, var obj: JsonObject)
data class MyJsonArray(var name: String, var obj: JsonArray)
data class MyChildObject(var className: String, var properties: List<MyDartProperties>, var index: Int? = null) :
    DartType()

data class FreezedPropertiesConfig(
    var setDefaultValue: Boolean = true,
    var useJsonKey: Boolean = true,
    var nameRuleRaw: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    var nameRuleTo: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    var hiveConfig: HiveSetting = HiveSetting()
) : DartPropertyGenerateConfig()

data class HiveSetting(var enable: Boolean = false, var hiveId: Int = 0)
data class FreezedClassConfig(
    var rootName: String = "Root",
    var saveFileName: String = "root",
    var saveDirectory: String = "",
    var addStructureFunction: Boolean = true,
    var addFromJsonFunction: Boolean = true,
    var propsConfig: FreezedPropertiesConfig = FreezedPropertiesConfig(),
    var formJsonType: FormJsonType? = FormJsonType.DynamicMap,
    var hiveSetting: HiveSetting = HiveSetting(),
    var runBuildCommand: Boolean = false,
    var openInEditor: Boolean = false,
    var useIsar: Boolean = false,
    var objectIndex: Int? = null,

    //命名规则
    var classNameFormatNew: NameFormatRule? = NameFormatRule.UPPER_CAMEL,
    var propertyNameFormatNew: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    //命名规则raw
    var classNameRaw: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    var propertyNameRaw: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    //freezed版本
    var freezedVersion: FreezedVersion = FreezedVersion.DefaultVersion,
    //freezed对象类型
    var freezedClassType: FreezedClassType = FreezedClassType.Sealed

) : DartClassGenerateConfig()

fun FreezedClassConfig.mainClassRun(mainCall: VoidCallback, otherRun: VoidCallback? = null) {
    if (objectIndex != null) {
        if (objectIndex == 0) {
            mainCall()
        } else {
            otherRun?.invoke()
        }
    }
}

@Service(Service.Level.PROJECT)
@State(name = "FlutterX Freezed Code Gen Setting", category = SettingsCategory.PLUGINS)
@Storage(roamingType = RoamingType.DEFAULT)
class FreezedClassConfigStateService(project: Project) : SimplePersistentStateComponent<FreezedClassConfig>(
    FreezedClassConfig(
        saveDirectory = project.guessProjectDir()?.path ?: ""
    )
) {
    companion object {
        fun getInstance(project: Project): FreezedClassConfigStateService = project.service()
    }
}

@Service(Service.Level.PROJECT)
@State(name = "FlutterX Dart Macro Code Gen Setting", category = SettingsCategory.PLUGINS)
@Storage(roamingType = RoamingType.DEFAULT)
class DartMarcoClassConfigStateService(project: Project) : SimplePersistentStateComponent<DartMarcoClassConfig>(
    DartMarcoClassConfig(
        saveDir = project.guessProjectDir()?.path ?: ""
    )
) {
    companion object {
        fun getInstance(project: Project): DartMarcoClassConfigStateService = project.service()
    }
}

///json数据处理
class MyJsonParseTool(root: JsonElement, var className: String = "Root") {

    val jsonObject: JsonObject? = if (root.isJsonObject) root.asJsonObject else null

    ///获取所有的字段(排除了对象和数组)
    private fun properties(): List<MyDartProperties> {
        val list = mutableListOf<MyDartProperties>()
        val map = jsonObject?.asMap() ?: emptyMap()
        map.onEachIndexed { index, (t, u) ->
            run {
                if (!u.isJsonObject && !u.isJsonArray) {
                    list.add(MyDartProperties(t.toString(), u.getDartType(), index))
                } else {
                    if (u.isJsonObject) {
                        list.add(MyDartProperties(t.toString(), DartCustomObject(t), index))
                    }
                    if (u.isJsonArray) {
                        val arr: JsonArray = u.asJsonArray
                        if (!arr.isEmpty) {
                            val first: JsonElement = arr.first()
                            when (val type = first.getDartType()) {
                                is DartCustomObject -> list.add(
                                    MyDartProperties(
                                        t.toString(),
                                        DartArrayValue(DartCustomObject(t).className.formatDartName()),
                                        index
                                    )
                                )

                                else -> list.add(MyDartProperties(t.toString(), DartArrayValue(type.dartType), index))
                            }
                        } else {
                            list.add(MyDartProperties(t.toString(), DartDynamicValue, index))
                        }
                    }
                }
            }
        }

        return list
    }

    ///获取所有属性
    fun findAllDartType(): List<DartType> {
        val list = mutableListOf<DartType>()
        if (jsonObject != null) {
            list.add(MyChildObject(className, properties()))
        }
        findAllObjects().forEach {
            val props = MyJsonParseTool(it.obj, it.name.formatDartName())
            list.addAll(props.findAllDartType())
        }
        findAllArrays().forEach {
            val maxObject: JsonElement? = it.obj.getMaxObject()
            if (maxObject != null) {
                val tool = MyJsonParseTool(maxObject, it.name.formatDartName())
                list.addAll(tool.findAllDartType())
            }
        }
        return list
    }


    ///获取值是对象的所有模型
    private fun findAllObjects(): List<MyJsonObject> {
        val list = mutableListOf<MyJsonObject>()
        jsonObject?.asMap()?.forEach { (t, u) ->
            run {
                if (u.isJsonObject) {
                    list.add(MyJsonObject(t.toString(), u.asJsonObject))
                }
            }
        }
        return list
    }

    ///获取所有的数组类型
    private fun findAllArrays(): List<MyJsonArray> {
        val list = mutableListOf<MyJsonArray>()
        jsonObject?.asMap()?.forEach { (t, u) ->
            run {
                if (u.isJsonArray) {
                    list.add(MyJsonArray(t.toString(), u.asJsonArray))
                }
            }
        }
        return list
    }

    companion object {

        fun parseJson(json: String): List<DartType> {
            val root = getRootJsonElement(json)
            return MyJsonParseTool(root).findAllDartType()
        }

        private fun getRootJsonElement(json: String): JsonElement {
            val ele = JsonParser.parseString(json)
            if (ele.isJsonObject) return ele
            if (ele.isJsonArray) {
                return ele.asJsonArray.getMaxObject() ?: ele
            }
            return ele
        }
    }
}


///获取dart类型
fun JsonElement.getDartType(): MyDartType {
    if (this.isJsonPrimitive) {
        val jp = this.asJsonPrimitive
        if (jp.isString) {
            return DartStringValue
        } else if (jp.isNumber) {
            return DartNumberValue
        } else if (jp.isBoolean) {
            return DartBooleanValue
        }
    }
    if (this.isJsonObject) {
        return DartCustomObject("")
    }
    return DartDynamicValue
}

fun JsonArray.getMaxObject(): JsonElement? {
    var maxItem = this.firstOrNull { it.isJsonObject }
    for (it in this.iterator()) {
        if (it.isJsonObject && maxItem != null) {
            if (it.asJsonObject.keySet().size > maxItem.asJsonObject.keySet().size) {
                maxItem = it
            }
        }
    }
    return maxItem
}


///生成freezed class
fun MyChildObject.getFreezedClass(config: FreezedClassConfig = FreezedClassConfig()): String {
    val sb = StringBuilder()
    val hiveConfig = config.hiveSetting
    val className = this.className.formatDartName(config.classNameRaw, config.classNameRaw)
    sb.appendLine("@freezed")


    //使用isar
    if (config.useIsar) {
        config.mainClassRun({
            sb.appendLine("@Collection(ignore: {'copyWith'})")
        }) {
            sb.appendLine("@Embedded(ignore: {'copyWith'})")
        }
    }

    if (hiveConfig.enable) {
        var id = hiveConfig.hiveId
        index?.let {
            if (it != 0) {
                id++
            }
        }
        sb.appendLine("@HiveType(typeId: $id)")
    }


    // freezed 3.x
    var freezedPre = ""
    when (config.freezedVersion) {
        FreezedVersion.DefaultVersion -> {}
        FreezedVersion.ThreeVersion -> {
            freezedPre = config.freezedClassType.type
        }
    }
    sb.appendLine("$freezedPre class $className with _\$$className {")
    if (config.addStructureFunction) {
        sb.appendLine("\tconst $className._();")
        sb.appendLine("")
    }
    sb.appendLine("\tconst factory $className({")


    ///添加isar属性
    if (config.useIsar) {
        config.mainClassRun({ sb.appendLine("\t\t@Default(Isar.autoIncrement) Id isarId,") })
    }

    ///属性
    properties.forEach { p ->
        val isLast = properties.last() == p
        sb.appendLine(
            "\t\t${
                p.getFreezedProperty(
                    config.propsConfig.copy(
                        hiveConfig = hiveConfig,
                        nameRuleRaw = config.propertyNameRaw,
                        nameRuleTo = config.propertyNameFormatNew
                    )
                )
            }${if (isLast) "" else ","}"
        )
    }


    sb.appendLine("\t}) = _$className;")
    sb.appendLine("")


    //添加 fromJson
    if (config.addFromJsonFunction) {
        sb.appendLine("\tfactory $className.fromJson(${(config.formJsonType ?: FormJsonType.DynamicMap).value} json) => _\$$className" + "FromJson(json);")
        sb.appendLine("")
    }

    if (config.useIsar) {
        config.mainClassRun({
            sb.appendLine("\tId get \$id => isarId; ")
            sb.appendLine("")
        })
    }

    sb.appendLine("}")
    return sb.toString()
}

fun MyDartProperties.getFreezedProperty(config: FreezedPropertiesConfig): String {
    val sb = StringBuilder()
    val hiveConfig = config.hiveConfig
    if (hiveConfig.enable) {
        val hiveDefaultValue = if (config.setDefaultValue) ", defaultValue:${this.type.defaultValue}" else ""
        sb.append("@HiveField($index$hiveDefaultValue) ")
    }

    if (config.useJsonKey) {
        sb.append("@JsonKey(name: '${this.name}') ")
    }

    if (config.setDefaultValue) {
        if (this.type.defaultValue.isNotBlank()) {
            sb.append("@Default(${this.type.defaultValue}) ")
        }
    } else {
        sb.append("required ")
    }

    sb.append(this.type.dartType + " ")

    sb.append(this.name.formatDartName(config.nameRuleRaw, config.nameRuleTo))


    return sb.toString()
}


fun String.formatDartName(raw: NameFormatRule?, to: NameFormatRule?): String {
    return (raw ?: NameFormatRule.LOWER_CAMEL).format.to((to ?: NameFormatRule.LOWER_CAMEL).format, this)
}


