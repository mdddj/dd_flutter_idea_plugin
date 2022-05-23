package shop.itbug.fluttercheckversionx.util

import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartTypeImpl

/**
 * psi 工具类类
 */
class MyDartPsiElementUtil {

    companion object {


        /**
         * @param referenceResolve 引用节点
         */
        fun getRefreshMethedName(referenceResolve: DartReferenceExpressionImpl): String {
            val resolve = referenceResolve.resolveScope
            val ref = referenceResolve.reference?.resolve()
            println(" referenceResolve: ${referenceResolve.elementType} resolve: ${resolve.javaClass} ref: $ref ")
//            if(resolve != null) {
//                val childrens = resolve.parent.children.filterIsInstance<DartTypeImpl>()
//                if(childrens.isNotEmpty()) {
//                    return  childrens.first().text
//                }
//            }
            return "dyanmic"
        }


    }

}