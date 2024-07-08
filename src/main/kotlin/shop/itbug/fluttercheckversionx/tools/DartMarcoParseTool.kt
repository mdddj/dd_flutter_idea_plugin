package shop.itbug.fluttercheckversionx.tools


data class DartMarcoPropertiesConfig(var setDefaultValue: Boolean = false) : DartPropertyGenerateConfig()
data class DartMarcoClassConfig(
    var openInEditor: Boolean = false,
    var propertiesConfig: DartMarcoPropertiesConfig = DartMarcoPropertiesConfig()
) : DartClassGenerateConfig()

///生成属性
fun MyDartProperties.generateDartMacro(config: DartMarcoPropertiesConfig = DartMarcoPropertiesConfig()): String {
    val sb = StringBuilder()
    sb.append("final ${this.type.dartType} ${this.name};")
    return sb.toString()
}

///生成class
fun MyChildObject.generateDartMacro(config: DartMarcoClassConfig = DartMarcoClassConfig()): String {
    val sb = StringBuilder()
    sb.appendLine("@JsonCodable()")
    sb.appendLine("class ${this.className}{")
    this.properties.forEach {
        sb.appendLine("\t${it.generateDartMacro(config.propertiesConfig)}")
    }
    sb.appendLine("}")

    return sb.toString()
}
