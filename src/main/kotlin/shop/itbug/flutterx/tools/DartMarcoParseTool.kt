package shop.itbug.flutterx.tools

import shop.itbug.flutterx.dialog.NameFormatRule


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

