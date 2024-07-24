package shop.itbug.fluttercheckversionx.tools


//生成dart macro 规则配置
data class DartMarcoClassConfig(
    var openInEditor: Boolean = false,
    var filename: String = "root",
    var saveDir: String = "",
    var classNameRule: NameFormat = NameFormat.Original,
    var propertiesNameRule: NameFormat = NameFormat.Original,
) : DartClassGenerateConfig()

///生成属性
fun MyDartProperties.generateDartMacro(config: DartMarcoClassConfig): String {
    val sb = StringBuilder()
    sb.append("final ${this.type.dartType} ${this.name.formatDartName(config.propertiesNameRule)};")
    return sb.toString()
}

///生成class
fun MyChildObject.generateDartMacro(config: DartMarcoClassConfig = DartMarcoClassConfig()): String {
    val sb = StringBuilder()
    sb.appendLine("@JsonCodable()")
    sb.appendLine("class ${this.className.formatDartName(config.classNameRule)}{")
    this.properties.forEach {
        sb.appendLine("\t${it.generateDartMacro(config)}")
    }
    sb.appendLine("}")

    return sb.toString()
}
