import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.manager.DartClassManager
import shop.itbug.fluttercheckversionx.manager.myManagerFun

class FreezedParseTest : BasePlatformTestCase() {


    override fun getTestDataPath(): String {
        return "src/test/testData"
    }


    fun testParseFreezedClass() {
        val file = myFixture.configureByFile("freezed.dart") as DartFile
        val dartClass = PsiTreeUtil.findChildrenOfAnyType(file, DartClassDefinitionImpl::class.java).toList()
        for (clazz in dartClass) {
            val manage: DartClassManager = clazz.myManagerFun()
            val allFactoryConstructorList = manage.getAllFactoryConstructorList()
            allFactoryConstructorList.forEach { factory ->

                manage.addFinalProperties(factory)
                println(file.text)
//                manage.addSimpleFactory(factory)

//                println(file.text)


//                val factoryManager: DartFactoryConstructorDeclarationImplManager = factory.manager()
//                val args = factoryManager.getNamedFields
//
//                args.forEach { arg: MyDartFieldModel ->
//                    val ele = arg.createFinalField(project)
//                    println("${ele?.text}")
//                    println("默认值:${arg.getDefaultValueString()}")
//                }
            }
        }
    }

}