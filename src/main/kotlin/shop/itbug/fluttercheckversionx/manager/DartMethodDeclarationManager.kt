package shop.itbug.fluttercheckversionx.manager

import com.jetbrains.lang.dart.psi.DartMethodDeclaration

val DartMethodDeclaration.myManager get() = DartMethodDeclarationManager(this)

class DartMethodDeclarationManager(val element: DartMethodDeclaration) : DartFactoryConstructorDeclarationInterface {
    init {
        println("return type: ${element.returnType?.text}")
    }

    override val componentNameList: List<String>
        get() = element.componentName?.run { listOf(this.name ?: "-") } ?: emptyList()
    override val getRequiredFields: List<MyDartFieldModel>
        get() = element.formalParameterList.normalFormalParameterList.map { it.myManager.myModel }
    override val getNamedFields: List<MyDartFieldModel>
        get() = element.formalParameterList.optionalFormalParameters?.defaultFormalNamedParameterList?.map { it.myManagerByNamed.myModel }
            ?: emptyList()
    override val getClassName: String
        get() = element.returnType?.text ?: (element.componentName?.name ?: "-")


}