package shop.itbug.fluttercheckversionx.model

import com.alibaba.fastjson2.JSONArray
import com.google.common.base.CaseFormat
import com.ibm.icu.math.BigDecimal
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil

fun FreezedCovertModel.getPropertiesString(): String {
    val sb = StringBuilder()
    if (properties.isNotEmpty()) {
        properties.forEach {
            var defaultValue = ""
            if (this.useDefaultValueIfNull) {
                defaultValue = "@Default(${it.getDartDefaultValue()}) "
            }

            val jsonKeyString = if (upperCamelStyle) "@JsonKey(name: '${it.finalPropertyName}') " else ""


            if (it.isNonNull) {
                sb.append(
                    "       $jsonKeyString$defaultValue${it.formatType(useDefaultValueIfNull)} ${
                        it.formatName(
                            upperCamelStyle
                        )
                    },"
                )
            } else {
                sb.append(
                    "      $jsonKeyString${
                        showRequiredString(
                            useDefaultValueIfNull,
                            defaultValue
                        )
                    } ${it.type} ${it.formatName(upperCamelStyle)},"
                )
            }
            sb.append("\n")
        }
    }
    return sb.toString()
}


fun showRequiredString(useDefaultValue: Boolean, defaultValue: String): String {
    return if (useDefaultValue.not()) {
        "required"
    } else {
        defaultValue
    }
}

fun DartClassProperty.formatName(upperCamelStyle: Boolean): String {
    if (upperCamelStyle) {
        var s = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, name)
        s = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_CAMEL, s)
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, s)
    }

    return name
}

fun DartClassProperty.formatType(useDefaultValue: Boolean): String {
    if (useDefaultValue) {
        return type.removeSuffix("?")
    }
    return type
}

/**
 * 如果为空设置为默认值
 */
fun DartClassProperty.getDartDefaultValue(): Any {
    when (finalPropertyValue) {
        is String -> {
            return "\'\'"
        }

        is Int -> {
            return "0"
        }

        is Boolean -> {
            return "false"
        }

        is BigDecimal -> {
            return "0.0"
        }

        is JSONArray -> {
            return "[]"
        }

        else -> return "$name()"
    }
}

fun DartVarDeclarationListImpl.covertDartClassPropertyModel(): DartClassProperty {
    return DartClassProperty(
        type = DartPsiElementUtil.getTypeWithVar(this),
        name = DartPsiElementUtil.getNameWithVar(this),
        isNonNull = DartPsiElementUtil.getTypeIsNonNull(this)
    )
}

data class DartClassProperty(
    val type: String, val name: String,
    //true - 可空
    //false - 不可为空
    val isNonNull: Boolean,
    //原始变量的值
    var finalPropertyValue: Any? = null,
    //原始变量key
    val finalPropertyName: String = "",
)

data class FreezedCovertModel(
    val properties: List<DartClassProperty>, var className: String,
    //变量使用驼峰命名法
    var upperCamelStyle: Boolean = true,
    //如果变量为空,则设置一个默认值
    var useDefaultValueIfNull: Boolean = false,

    )