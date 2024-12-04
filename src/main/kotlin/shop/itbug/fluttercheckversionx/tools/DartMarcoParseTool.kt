package shop.itbug.fluttercheckversionx.tools

import shop.itbug.fluttercheckversionx.dialog.NameFormatRule


//生成dart macro 规则配置
data class DartMarcoClassConfig(
    var openInEditor: Boolean = false,
    var filename: String = "root",
    var saveDir: String = "",
    var classNameRuleNew: NameFormatRule? = NameFormatRule.UPPER_CAMEL,
    var propertiesNameRuleNew: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
    var classNameRuleRaw: NameFormatRule? = NameFormatRule.UPPER_CAMEL,
    var propertiesNameRuleRaw: NameFormatRule? = NameFormatRule.LOWER_CAMEL,
) : DartClassGenerateConfig()

///生成属性
fun MyDartProperties.generateDartMacro(config: DartMarcoClassConfig): String {
    val sb = StringBuilder()
    sb.append(
        "final ${this.type.dartType} ${
            this.name.formatDartName(
                config.propertiesNameRuleRaw,
                config.propertiesNameRuleNew
            )
        };"
    )
    return sb.toString()
}

///生成class
fun MyChildObject.generateDartMacro(config: DartMarcoClassConfig = DartMarcoClassConfig()): String {
    val sb = StringBuilder()
    sb.appendLine("@JsonCodable()")
    sb.appendLine("class ${this.className.formatDartName(config.classNameRuleRaw, config.classNameRuleNew)}{")
    this.properties.forEach {
        sb.appendLine("\t${it.generateDartMacro(config)}")
    }
    sb.appendLine("}")

    return sb.toString()
}
