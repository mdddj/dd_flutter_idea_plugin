package shop.itbug.flutterx.manager

import com.jetbrains.lang.dart.psi.impl.DartNamedConstructorDeclarationImpl

val DartNamedConstructorDeclarationImpl.myManager get() = DartNamedConstructorDeclarationImplManager(this)

class DartNamedConstructorDeclarationImplManager(val element: DartNamedConstructorDeclarationImpl) :
    DartFactoryConstructorDeclarationInterface {
    override val getClassName: String
        get() {
            return element.componentNameList.firstOrNull()?.name ?: "-"
        }
    override val componentNameList: List<String>
        get() = element.componentNameList.filter { it.name != null }.map { it.name ?: "" }
    override val getRequiredFields: List<MyDartFieldModel>
        get() = element.formalParameterList.normalFormalParameterList.map { it.myManager.myModel }
    override val getNamedFields: List<MyDartFieldModel>
        get() = element.formalParameterList.optionalFormalParameters?.defaultFormalNamedParameterList?.map { it.myManagerByNamed.myModel }
            ?: emptyList()


}