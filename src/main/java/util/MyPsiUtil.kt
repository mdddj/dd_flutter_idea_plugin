package util

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

/**
 * PSI 操作相关类
 */
class MyPsiElementUtil {

    companion object {

        /**
         * 获取插件名字
         *
         * 例子:
         * flutter_launcher_icons: ^0.9.2
         * 返回 flutter_launcher_icons
         */
        fun getPluginNameWithPsi(psiElement: PsiElement?): String {
            if (psiElement != null) {
                val text = psiElement.text
                if (text != null) {
                    if ( psiElement is YAMLKeyValueImpl && (text.contains(": ^") || text.contains(": any")) ) {
                        return text.split(":")[0]
                    }
                }
            }
            return ""
        }
    }
}