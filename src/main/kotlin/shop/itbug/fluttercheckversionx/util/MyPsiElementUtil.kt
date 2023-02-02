package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import com.jetbrains.lang.dart.util.DartElementGenerator
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel

typealias CreatePsiFileSuccess = (psiFile: PsiFile) -> Unit

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
        private fun createVarExpressionFromText(project: Project, text: String): DartVarDeclarationListImpl {
            val psiFile = DartElementGenerator.createDummyFile(project, text)
            return PsiTreeUtil.getChildOfType(psiFile, DartVarDeclarationListImpl::class.java)!!
        }

        /**
         * 创建逗号
         */
        private fun createLeafPsiElement(project: Project): LeafPsiElement {
            val file = DartElementGenerator.createDummyFile(project, "var d = 'hello';")
            return PsiTreeUtil.getChildOfType(file, LeafPsiElement::class.java)!!
        }

        /**
         * 根据类名创建PsiElement
         */
        fun createDartClassBodyFromClassName(project: Project, className: String): DartClassDefinitionImpl {
            val file = DartElementGenerator.createDummyFile(project, "class $className{\n\n}")
            return PsiTreeUtil.getChildOfType(file, DartClassDefinitionImpl::class.java)!!
        }

        /**
         * 创建一个dart file
         */
        private fun createDartFileWithElement(
            project: Project,
            element: PsiElement,
            path: String,
            filename: String,
            onSuccess: CreatePsiFileSuccess?
        ): PsiFile? {
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
                        onSuccess?.invoke(e)
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
        private fun checkFileIsExits(
            project: Project,
            path: String,
            existenceHandle: (psiFile: PsiFile) -> Unit
        ): Boolean {
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


        /**
         * 自动生成资产文件
         * @param project 项目
         * @param name 扫描目录名字,比如 "assets"
         */
        fun autoGenerateAssetsDartClassFile(
            project: Project,
            name: String,
            auto: Boolean = false,
            config: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
        ) {
            val names = mutableSetOf<String>()
            val classElement =
                createDartClassBodyFromClassName(project, config.className)
            classElement.classBody?.classMembers?.let { classMembers ->
                MyFileUtil.onFolderEachWithProject(project, name) { virtualFile ->
                    val eleValue = virtualFile.fileNameWith(name)
                    var filename = Util.removeSpecialCharacters(virtualFile.presentableName.split(".").first())
                    if (names.contains(filename)) filename += "${names.filter { it.contains(filename) }.size}"
                    if (filename.isNotEmpty() && eleValue.isNotEmpty()) {
                        val expression = "static const $filename = '$eleValue'"
                        names.add(filename)
                        val ex = createVarExpressionFromText(
                            project,
                            expression
                        )
                        val d = createLeafPsiElement(project)
                        runWriteAction {
                            ex.addAfter(d, ex.nextSibling)
                            classMembers.addAfter(ex, classMembers.nextSibling)
                        }

                    }
                }
            }

            val file =
                createDartFileWithElement(project, classElement, config.path, "${config.fileName}.dart", onSuccess = {
                    project.toast(if (auto) "监听到资产文件夹变化,自动生成成功." else "生成成功:${it.name}")
                })

            file?.let {
                WriteCommandAction.runWriteCommandAction(project) {
                    CodeStyleManager.getInstance(project).reformat(file)
                }
            }
        }

        fun generateDartMetadata(name: String, project: Project): DartMetadataImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(
                project, "@$name\n" +
                        "class A{}"
            )
            return PsiTreeUtil.findChildOfType(createDummyFile, DartMetadataImpl::class.java)!!
        }

        fun generateSpace(project: Project, text: String = "\n"): PsiWhiteSpaceImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(project, text)
            return PsiTreeUtil.findChildOfType(createDummyFile, PsiWhiteSpaceImpl::class.java)!!
        }

        fun generateMixins(project: Project, name: String): DartMixinsImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(project, "class A with $name")
            return PsiTreeUtil.findChildOfType(createDummyFile, DartMixinsImpl::class.java)!!
        }


        fun genFreezedClass(project: Project, className: String, properties: String = ""): PsiFile {
            return DartElementGenerator.createDummyFile(
                project, "@freezed\n" +
                        "class $className with _\$$className {\n" +
                        "  const factory $className({\n$properties    }) = _$className;\n" +
                        "  \n  factory $className.fromJson(Map<String, dynamic> json) => _\$${className}FromJson(json);\n" +
                        "}"
            )
        }

        fun freezedGetDartFactoryConstructorDeclarationImpl(file: PsiFile): DartFactoryConstructorDeclarationImpl {
            return PsiTreeUtil.findChildOfType(file, DartFactoryConstructorDeclarationImpl::class.java)!!
        }

        /**
         * 生成可空的属性
         */
        fun getNullProperties(type: String, name: String, project: Project): DartDefaultFormalNamedParameterImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(
                project, "class B {\n" +
                        "  B({$type $name});\n" +
                        "}"
            )
            return PsiTreeUtil.getChildOfType(createDummyFile, DartDefaultFormalNamedParameterImpl::class.java)!!
        }
    }


}