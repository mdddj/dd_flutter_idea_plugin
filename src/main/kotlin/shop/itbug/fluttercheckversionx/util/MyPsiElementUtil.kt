package shop.itbug.fluttercheckversionx.util

import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl

/**
 * psi 工具类类
 */
class MyDartPsiElementUtil {

    companion object {


        /**
         * @param referenceResolve 引用节点
         */
        fun getRefreshMethedName(referenceResolve: DartReferenceExpressionImpl): String {
           val dartData =   DartAnalysisServerService.getInstance(referenceResolve.project).analysis_getHover(
                referenceResolve.containingFile.virtualFile,
                referenceResolve.textOffset
            )
            return dartData.firstOrNull()?.staticType.toString()
        }
    }

}