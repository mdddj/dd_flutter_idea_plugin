package shop.itbug.fluttercheckversionx.constance

import com.intellij.openapi.util.Key
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl

class MyKeys {
    companion object {
        val DartClassKey = Key.create<DartClassDefinitionImpl>("CurrentClass")
    }
}