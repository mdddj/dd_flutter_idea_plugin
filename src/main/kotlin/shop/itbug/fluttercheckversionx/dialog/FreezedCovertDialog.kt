package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartOptionalFormalParametersImpl
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.addAnnotation
import shop.itbug.fluttercheckversionx.util.addMixin
import javax.swing.JComponent

class FreezedCovertDialog(val project: Project, val model: FreezedCovertModel) : DialogWrapper(project) {
    init {
        super.init()
        title = "模型转Freezed对象(${model.className})"
        setSize(500, 380)

    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                button("生成") {
                    generateFreezedModel()
                }
            }
        }
    }


    /**
     * 生成freezed类
     */
    private fun generateFreezedModel() {
        val classBodyFromClassName =
            MyDartPsiElementUtil.createDartClassBodyFromClassName(project, model.className)
                .addAnnotation("freezed", project)
                .addMixin("_\$${model.className}", project)

        val genFreezedClass = MyDartPsiElementUtil.genFreezedClass(project, model.className)
        val factoryBody =
            MyDartPsiElementUtil.freezedGetDartFactoryConstructorDeclarationImpl(genFreezedClass)
        val properties = PsiTreeUtil.findChildOfType(factoryBody, DartOptionalFormalParametersImpl::class.java)!!



        println(classBodyFromClassName.text)

    }

}