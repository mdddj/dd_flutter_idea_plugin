package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import com.jetbrains.lang.dart.util.DartElementGenerator

/**
 * psi 工具类类
 */
class MyDartPsiElementUtil {

    companion object {

        /**
         * @param referenceResolve 引用节点
         */
        fun getRefreshMethodName(referenceResolve: DartReferenceExpressionImpl): String {
            val dartData = DartAnalysisServerService.getInstance(referenceResolve.project).analysis_getHover(
                referenceResolve.containingFile.virtualFile,
                referenceResolve.textOffset
            )
            return dartData.firstOrNull()?.staticType.toString()
        }


        /**
         * 创建var表达式
         */
        fun createVarExpressionFromText(project: Project, text: String): DartVarDeclarationListImpl? {
            val psiFile = DartElementGenerator.createDummyFile(project, text)
            return PsiTreeUtil.getChildOfType(psiFile, DartVarDeclarationListImpl::class.java)
        }

        /**
         * 创建逗号
         */
        fun createLeafPsiElement(project: Project): LeafPsiElement {
            val file = DartElementGenerator.createDummyFile(project, "var d = 'hello';")
            return PsiTreeUtil.getChildOfType(file, LeafPsiElement::class.java)!!
        }

        /**
         * 根据类名创建PsiElement
         */
        fun createDartClassBodyFromClassName(project: Project, className: String): DartClassDefinitionImpl {
            val file = DartElementGenerator.createDummyFile(project, "class $className{}")
            return PsiTreeUtil.getChildOfType(file, DartClassDefinitionImpl::class.java)!!
        }

        /**
         * 创建一个dart file
         */
        fun createDartFileWithElement(project: Project, element: PsiElement, path: String, filename: String): PsiFile? {
            val findFileByPath = LocalFileSystem.getInstance().findFileByPath(project.basePath + "/" + path)
            if (findFileByPath != null) {
                val findDirectory = PsiManager.getInstance(project).findDirectory(findFileByPath)
                if (findDirectory != null) {
                    checkFileIsExits(project, "$path/$filename") {
                        it.delete()
                    }
                    val e = PsiFileFactory.getInstance(project)
                        .createFileFromText(filename, DartLanguage.INSTANCE, element.text)
                    runWriteAction {
                        findDirectory.add(e)
                        project.toast("生成成功")
                    }
                    return e
                } else {
                    project.toastWithError("查找目录失败")
                }
            } else {
                project.toastWithError("查找目录失败")
            }
            return null
        }

        ///检测文件是否存在
        fun checkFileIsExits(project: Project, path: String, existenceHandle: (psiFile: PsiFile) -> Unit): Boolean {
            val file = LocalFileSystem.getInstance().findFileByPath(project.basePath + "/" + path)
            if (file != null) {
                val findFile = PsiManager.getInstance(project).findFile(file)
                if (findFile != null) {
                    runWriteAction {
                        existenceHandle.invoke(findFile)
                    }
                    return true
                }
            }
            return false

        }


        /**
         * 检测是否有相同的PsiElement
         * 返回true 表示有相同的
         * false 则没有
         */
        fun <T : PsiElement> checkElementEqName(
            project: Project,
            element: PsiElement,
            text: String,
            type: Class<T>
        ): Boolean {
            println(element.children.size)
            val file = DartElementGenerator.createDummyFile(project, element.text)
            val childrenOfAnyType = PsiTreeUtil.getChildrenOfAnyType(file.originalElement, type)
            println(">>${childrenOfAnyType.size}")
            return childrenOfAnyType.any {
                it.text.equals(text)
            }
        }

    }

}