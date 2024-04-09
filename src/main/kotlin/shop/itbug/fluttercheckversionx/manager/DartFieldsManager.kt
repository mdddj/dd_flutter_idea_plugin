package shop.itbug.fluttercheckversionx.manager

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartDefaultFormalNamedParameter
import com.jetbrains.lang.dart.psi.DartNormalFormalParameter
import com.jetbrains.lang.dart.psi.impl.DartFieldFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartSimpleFormalParameterImpl
import shop.itbug.fluttercheckversionx.document.getDartElementType


interface FieldManager {
    val myModel: MyDartFieldModel
}

val MyDartFieldModel.fieldString: String
    get() {
        return "$typeString $fieldNameString"
    }

/**
 * 封装为模型
 */
data class MyDartFieldModel(

    //类型字符串
    val typeString: String,
    //类型属性的名称
    val fieldNameString: String,
    //是否可以为null,true:可以为null
    val isOption: Boolean = false,
)

/**
 * dart参数的处理
 */
val DartNormalFormalParameter.myManager get() = DartFieldsManager(this)

class DartFieldsManager(val element: DartNormalFormalParameter) : FieldManager {

    /**
     * 构建为我的模型
     */
    override val myModel: MyDartFieldModel
        get() {
            var typeString = ""
            var fieldNameString = ""
            val simpleParam = PsiTreeUtil.findChildOfType(element, DartSimpleFormalParameterImpl::class.java)
            if (simpleParam != null) {
                simpleParam.type?.let {
                    typeString = it.text
                }
                fieldNameString = simpleParam.componentName.text
            }
            val f = PsiTreeUtil.findChildOfType(element, DartFieldFormalParameterImpl::class.java)
            if (f != null) {
                typeString = f.referenceExpression.getDartElementType() ?: (f.type?.text ?: "-")
                fieldNameString = f.referenceExpression.text
                println("fieldNameString: $fieldNameString,typeString: $typeString")
            }
            return MyDartFieldModel(typeString, fieldNameString, typeString.endsWith("?"))
        }
}

val DartDefaultFormalNamedParameter.myManagerByNamed get() = DartNamedFieldsManager(this)

class DartNamedFieldsManager(val element: DartDefaultFormalNamedParameter) : FieldManager {
    override val myModel: MyDartFieldModel
        get() {
            return element.normalFormalParameter.myManager.myModel
        }

}