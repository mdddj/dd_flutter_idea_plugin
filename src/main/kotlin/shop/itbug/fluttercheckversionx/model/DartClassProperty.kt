package shop.itbug.fluttercheckversionx.model

import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil

fun DartVarDeclarationListImpl.covertDartClassPropertyModel() : DartClassProperty{
    return DartClassProperty(
        type = DartPsiElementUtil.getTypeWithVar(this),
        name = DartPsiElementUtil.getNameWithVar(this),
        isNonNull = DartPsiElementUtil.getTypeIsNonNull(this)
    )
}

data class DartClassProperty(val type: String, val name: String,val isNonNull: Boolean)
data class FreezedCovertModel(val properties: List<DartClassProperty>, val className: String)