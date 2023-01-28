package shop.itbug.fluttercheckversionx.model

import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil

fun FreezedCovertModel.getPropertiesString(): String {
    val sb = StringBuilder()
    if (properties.isNotEmpty()) {
        properties.forEach {
            sb.append(if (it.isNonNull) "       ${it.type} ${it.name}," else "      required ${it.type} ${it.name},")
            sb.append("\n")
        }
    }
    return sb.toString()
}

fun DartVarDeclarationListImpl.covertDartClassPropertyModel(): DartClassProperty {
    return DartClassProperty(
        type = DartPsiElementUtil.getTypeWithVar(this),
        name = DartPsiElementUtil.getNameWithVar(this),
        isNonNull = DartPsiElementUtil.getTypeIsNonNull(this)
    )
}

data class DartClassProperty(val type: String, val name: String, val isNonNull: Boolean)
data class FreezedCovertModel(val properties: List<DartClassProperty>, val className: String)