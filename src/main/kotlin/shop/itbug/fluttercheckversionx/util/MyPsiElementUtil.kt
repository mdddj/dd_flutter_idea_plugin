package shop.itbug.fluttercheckversionx.util

import com.google.common.base.CaseFormat
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
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
import java.io.File

typealias CreatePsiFileSuccess = (psiFile: PsiFile) -> Unit

class CreatePsiElementException(message: String) : Exception(message)

fun Project.reformat(element: PsiElement) {
    WriteCommandAction.runWriteCommandAction(this) {
        CodeStyleManager.getInstance(this).reformat(element)
    }
}

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
            val file = DartElementGenerator.createDummyFile(
                project, "class $className{\n" +
                        "\n}"
            )
            return PsiTreeUtil.getChildOfType(file, DartClassDefinitionImpl::class.java)!!
        }

        /**
         * 创建一个dart file
         */
        fun createDartFileWithElement(
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
                    val dartFile = PsiFileFactory.getInstance(project)
                        .createFileFromText(filename, DartLanguage.INSTANCE, element.text)
                    project.reformat(dartFile)
                    project.toast("文件已经格式化成功.")
                    runWriteAction {
                        findDirectory.add(dartFile)
                        onSuccess?.invoke(dartFile)
                    }
                    return dartFile
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
            userSetting: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
        ) {

            val names: MutableList<String> = mutableListOf()

            /**
             * 格式化属性名
             */
            fun formatName(
                file: VirtualFile,
                setting: GenerateAssetsClassConfigModel,
                project: Project,
                names: MutableList<String>
            ): String {
                var initFilename = file.nameWithoutExtension //默认就是文件名
                //命名添加前缀
                if (setting.addFolderNamePrefix) {
                    initFilename = file.path
                        .removeSuffix(file.extension ?: "")
                        .replace(project.basePath ?: "", "")
                        .replace(File.separator, "_")
                        .removePrefix("_")
                }

                //命名添加类型后缀
                if (setting.addFileTypeSuffix) {
                    initFilename += file.extension
                }


                //进行特殊字符替换
                val split = setting.replaceTags.split(",")
                split.forEach {
                    initFilename = initFilename.replace(it, "_")
                }

                //判断是否有中文
                if (Util.isContainChinese(initFilename)) {
                    project.toast("梁典典:[${file.name}]包含中文字符已被忽略")
                    return ""
                }
                //进行命名格式化,首字母大写
                initFilename = if (setting.firstChatUpper) {
                    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, initFilename)
                } else {
                    CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, initFilename)
                }

                //判断是否有重名的
                if (names.contains(initFilename)) {
                    val size = names.filter { it.contains(initFilename) }.size
                    initFilename += "${size + 1}"
                }

                //过滤忽略掉的
                if (setting.igFiles.contains(file.name)) {
                    project.toast("文件[${file.name}] 已被忽略")
                    return ""
                }
                return initFilename
            }

            val classElement = createDartClassBodyFromClassName(project, userSetting.className)

            classElement.classBody?.classMembers?.let { classMembers ->
                MyFileUtil.onFolderEachWithProject(project, name) { virtualFile ->
                    val attrName = formatName(virtualFile, userSetting, project, names)//属性名
                    if (attrName.isNotEmpty()) {
                        val eleValue = virtualFile.fileNameWith(name)//属性值
                        val expression = "static const $attrName = '$eleValue'"
                        names.add(attrName)
                        createVarExpressionFromText(
                            project,
                            expression
                        ).let {
                            val d = createLeafPsiElement(project)
                            runWriteAction {
                                it?.addAfter(d, it.nextSibling)
                                it?.let { it1 -> classMembers.addAfter(it1, classMembers.nextSibling) }
                            }
                        }
                    }
                }
            }

            project.reformat(classElement)

            createDartFileWithElement(
                project,
                classElement,
                "lib",
                "${userSetting.fileName}.dart",
                null
            )
            names.clear()
            if (auto) {
                project.toast("梁典典:自动生成资产类成功")
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


        fun getWidgetRefParam(project: Project): DartFormalParameterListImpl {
            val createFile = DartElementGenerator.createDummyFile(
                project, """
                void test(BuildContext context,WidgetRef ref){}
            """.trimIndent()
            )
            return PsiTreeUtil.findChildOfType(createFile, DartFormalParameterListImpl::class.java)!!
        }


        fun generateMixins(project: Project, name: String): DartMixinsImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(project, "class A with $name")
            return PsiTreeUtil.findChildOfType(createDummyFile, DartMixinsImpl::class.java)!!
        }


        fun genFreezedClass(
            project: Project,
            className: String,
            properties: String = "",
            addConstructor: Boolean = true,
            addFromJson: Boolean = true
        ): PsiFile {
            return DartElementGenerator.createDummyFile(
                project, "@freezed\n" +
                        "class $className with _\$$className {\n" +
                        (if (addConstructor) "  const $className._();\n\n" else "") +

                        "  const factory $className({\n$properties    }) = _$className;\n" +
                        (if (addFromJson) "  \n  factory $className.fromJson(Map<String, dynamic> json) => _\$${className}FromJson(json);\n\n" else "") +
                        "}"
            )
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


        /**
         * 创建dart类节点
         */
        fun createDartNamePsiElement(name: String, project: Project): DartComponentNameImpl {
            val createDummyFile = DartElementGenerator.createDummyFile(project, "class $name {}")!!
            ///扫描子节点中的元素
            val elements =
                MyPsiElementUtil.findAllMatchingElements(createDummyFile) { _: String, psiElement: PsiElement ->
                    psiElement is DartComponentNameImpl
                }
            return elements.firstOrNull() as? DartComponentNameImpl
                ?: throw CreatePsiElementException("MyDartPsiElementUtil.createDartNamePsiElement Not working properly")

        }


        /**
         *
         * 创建DartTypeImpl
         * class Test {
         *   factory Test() = _Test;
         * }
         */
        fun createDartTypeImplElement(name: String, project: Project): DartTypeImpl {
            val dummy = DartElementGenerator.createDummyFile(
                project, "class Test {\n" +
                        "  factory Test() = $name;\n" +
                        "}"
            )
            val elements =
                MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
                    psiElement is DartTypeImpl
                }
            return elements.firstOrNull() as? DartTypeImpl
                ?: throw CreatePsiElementException("MyDartPsiElementUtil.createDartTypeImplElement Not working properly")
        }


        /**
         * 创建结构体
         */
        fun createFunBody(name: String, project: Project): DartFactoryConstructorDeclarationImpl {
            val dummy = DartElementGenerator.createDummyFile(
                project, "class Test {\n" +
                        "  $name\n" +
                        "}"
            )
            val elements =
                MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
                    psiElement is DartFactoryConstructorDeclarationImpl
                }
            return elements.firstOrNull() as? DartFactoryConstructorDeclarationImpl
                ?: throw CreatePsiElementException("MyDartPsiElementUtil.createFunBody Not working properly")
        }

        /**
         * 创建结构体
         */
        fun createMixin(name: String, project: Project): DartTypeListImpl {
            val dummy = DartElementGenerator.createDummyFile(
                project, """
                    class Test with $name{}
                """.trimIndent()
            )
            val elements =
                MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
                    psiElement is DartTypeListImpl
                }
            return elements.firstOrNull() as? DartTypeListImpl
                ?: throw CreatePsiElementException("MyDartPsiElementUtil.createMixin Not working properly")
        }

        fun createDartDartReferenceExpressionImplPsiElement(
            name: String,
            project: Project
        ): DartReferenceExpressionImpl {
            val file = DartElementGenerator.createDummyFile(
                project, "class $name{" +
                        "}" +
                        "var b = $name();"
            )!!
            return PsiTreeUtil.getChildOfType(file, DartReferenceExpressionImpl::class.java)!!

        }


        ///生成一个单例类
        fun genClassConstructor(project: Project, className: String): PsiFile? {
            val text = """
                class $className {
                  $className._();
                  static $className get _instance => $className._();
                  factory $className() => _instance;
                }
            """.trimIndent()
            return DartElementGenerator.createDummyFile(project, text)
        }


        fun createDartPart(text: String, project: Project): DartPartStatementImpl? {
            val createDummyFile = DartElementGenerator.createDummyFile(project, text)
            return PsiTreeUtil.getChildOfType(createDummyFile, DartPartStatementImpl::class.java)
        }


    }


}


