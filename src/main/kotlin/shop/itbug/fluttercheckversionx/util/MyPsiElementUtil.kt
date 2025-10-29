package shop.itbug.fluttercheckversionx.util

import com.google.common.base.CaseFormat
import com.intellij.ide.util.gotoByName.GotoClassModel2
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.*
import com.jetbrains.lang.dart.util.DartElementGenerator
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.services.PubspecService
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
object MyDartPsiElementUtil {

    /**
     * 根据文本来搜索类
     */
    fun searchClassByText(project: Project, text: String): DartComponentNameImpl? {

        //去除可空类型
        val search = if (text.endsWith("?")) {
            text.removeSuffix("?")
        } else {
            text
        }
        val gotoClassModel2 = GotoClassModel2(project)
        val elements = gotoClassModel2.getElementsByName(search, true, search)
        return elements.firstOrNull() as? DartComponentNameImpl
    }


    /**
     * @param referenceResolve 引用节点
     */
    fun getRefreshMethodName(referenceResolve: DartReferenceExpressionImpl): String {
        val dartData = DartAnalysisServerService.getInstance(referenceResolve.project).analysis_getHover(
            referenceResolve.containingFile.virtualFile, referenceResolve.textOffset
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
            project, "class $className{\n\n}"
        )
        return PsiTreeUtil.getChildOfType(file, DartClassDefinitionImpl::class.java)!!
    }

    /**
     * 创建一个dart file
     */
    fun createDartFileWithElement(
        project: Project, element: PsiElement, path: String, filename: String, onSuccess: CreatePsiFileSuccess?
    ): PsiFile? {
        val findFileByPath = LocalFileSystem.getInstance().findFileByPath(path)
        if (findFileByPath != null) {
            val findDirectory = PsiManager.getInstance(project).findDirectory(findFileByPath)
            if (findDirectory != null) {
                checkFileIsExits(project, "$path${File.separator}$filename") {
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
        project: Project, path: String, existenceHandle: (psiFile: PsiFile) -> Unit
    ): Boolean {
        val file = LocalFileSystem.getInstance().findFileByPath(path)
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
     * 自动生成资产文件
     * @param project 项目
     * @param name 扫描目录名字,比如 "assets"
     */
    fun autoGenerateAssetsDartClassFile(
        project: Project,
        name: String,
        auto: Boolean = false,
        userSetting: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getGenerateAssetsSetting(project)
    ) {

        val names: MutableList<String> = mutableListOf()

        /**
         * 格式化属性名
         */
        fun formatName(
            file: VirtualFile, setting: GenerateAssetsClassConfigModel, project: Project, names: MutableList<String>
        ): String {
            var initFilename = file.nameWithoutExtension //默认就是文件名
            //命名添加前缀
            if (setting.addFolderNamePrefix) {
                initFilename = file.path.removeSuffix(file.extension ?: "").replace(project.basePath ?: "", "")
                    .replace(File.separator, "_").removePrefix("_")
            }

            //命名添加类型后缀
            if (setting.addFileTypeSuffix) {
                initFilename += file.extension
            }


            //进行特殊字符替换
            val split = (setting.replaceTags ?: "").split(",")
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

        val classElement = createDartClassBodyFromClassName(project, userSetting.className ?: "")

        classElement.classBody?.classMembers?.let { classMembers ->
            MyFileUtil.onFolderEachWithProject(project, name) { virtualFile ->
                val attrName = formatName(virtualFile, userSetting, project, names)//属性名
                if (attrName.isNotEmpty()) {
                    val eleValue = virtualFile.fileNameWith(name)//属性值
                    val expression = "static const $attrName = '$eleValue'"
                    names.add(attrName)
                    createVarExpressionFromText(
                        project, expression
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
            project, classElement, "${userSetting.path}", "${userSetting.fileName}.dart", null
        )
        names.clear()
        if (auto) {
            project.toast("Success")
        }
    }

    fun generateDartMetadata(name: String, project: Project): DartMetadataImpl {
        val createDummyFile = DartElementGenerator.createDummyFile(
            project, "@$name\nclass A{}"
        )
        return PsiTreeUtil.findChildOfType(createDummyFile, DartMetadataImpl::class.java)!!
    }


    fun getWidgetRefParam(project: Project): DartFormalParameterListImpl {
        val createFile = DartElementGenerator.createDummyFile(
            project, """
                void test(BuildContext context,WidgetRef ref){}
            """.trimIndent()
        )
        return PsiTreeUtil.findChildOfType(createFile, DartFormalParameterListImpl::class.java)!!
    }


    fun genFreezedClass(
        project: Project,
        className: String,
        properties: String = "",
        addConstructor: Boolean = true,
        addFromJson: Boolean = true
    ): PsiFile {

        //判断是不是 freezed 3.0
        val isThan3 = PubspecService.getInstance(project).freezedVersionIsThan3()
        return DartElementGenerator.createDummyFile(
            project,
            "@freezed\n" + "${if (isThan3) "sealed" else ""} class $className with _$$className {\n" + (if (addConstructor) "  const $className._();\n\n" else "") +

                    "  const factory $className({\n$properties    }) = _$className;\n" + (if (addFromJson) "  \n  factory $className.fromJson(Map<String, dynamic> json) => _$${className}FromJson(json);\n\n" else "") + "}"
        )
    }


    /**
     * 创建dart类节点
     */
    fun createDartNamePsiElement(name: String, project: Project): DartComponentNameImpl {
        val createDummyFile = DartElementGenerator.createDummyFile(project, "class $name {}")!!
        ///扫描子节点中的元素
        val elements = MyPsiElementUtil.findAllMatchingElements(createDummyFile) { _: String, psiElement: PsiElement ->
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
            project, "class Test {\n  factory Test() = $name;\n}"
        )
        val elements = MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
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
            project, "class Test {\n  $name\n}"
        )
        val elements = MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
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
        val elements = MyPsiElementUtil.findAllMatchingElements(dummy) { _: String, psiElement: PsiElement ->
            psiElement is DartTypeListImpl
        }
        return elements.firstOrNull() as? DartTypeListImpl
            ?: throw CreatePsiElementException("MyDartPsiElementUtil.createMixin Not working properly")
    }


    fun createDartPart(text: String, project: Project): DartPartStatementImpl? {
        val createDummyFile = DartElementGenerator.createDummyFile(project, text)
        return PsiTreeUtil.getChildOfType(createDummyFile, DartPartStatementImpl::class.java)
    }


    /**
     * dart文件:
     * 检查import语句是否存在
     * @param importText 例子: package:flutter/cupertino.dart
     */
    fun checkImportIsExist(psiFile: PsiFile, importText: String): Boolean {
        if (psiFile !is DartFile) return false
        val importElements =
            runReadAction { PsiTreeUtil.findChildrenOfType(psiFile, DartImportStatementImpl::class.java) }
        if (importElements.isEmpty()) return false
        val find = importElements.find { it.uriElement.text.replace("\"", "").replace("\'", "") == importText }
        return find != null
    }

    /**
     * 创建一个导入语句
     * @param importText 例子: package:flutter/cupertino.dart
     */
    fun createImportStatement(importText: String, project: Project): DartImportStatementImpl {
        val createDummyFile = DartElementGenerator.createDummyFile(project, "import \'$importText\';")
        return runReadAction { PsiTreeUtil.findChildOfType(createDummyFile, DartImportStatementImpl::class.java)!! }
    }


    /**
     * 向dart文件中插入一条导入语句
     */
    fun insertImportStatement(project: Project, psiFile: PsiFile, importText: String) {
        commitPsiFile(psiFile, project)
        val createPsiElement = createImportStatement(importText, project)
        WriteCommandAction.runWriteCommandAction(project) {
            psiFile.addBefore(createPsiElement, psiFile.firstChild)
        }
    }


    /**
     * 保存文档
     */
    fun commitPsiFile(psiFile: PsiFile, project: Project) {
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        document?.let { PsiDocumentManager.getInstance(project).commitDocument(it) }
    }

    /**
     * 添加riverpod hooks 语句
     */
    fun addRiverpodHookImport(psiFile: PsiFile, project: Project) {
        val config = PluginConfig.getState(project)
        val packageText = config.autoImportRiverpodText ?: ""
        val exist = checkImportIsExist(
            psiFile, packageText
        )
        if (exist.not()) {
            insertImportStatement(
                project, psiFile, packageText
            )
        }
    }


    fun createSealedPsiElement(project: Project): PsiElement {
        val createDummyFile = DartElementGenerator.createDummyFile(
            project, "sealed class Test {}".trimIndent()
        ) as DartFile
        return createDummyFile.originalElement.firstChild.node.findChildByType(DartTokenTypes.SEALED)?.psi!!
    }


    fun createAbstractPsiElement(project: Project): PsiElement {
        val createDummyFile = DartElementGenerator.createDummyFile(
            project, """
            abstract class Test {}
        """.trimIndent()
        ) as DartFile
        return createDummyFile.originalElement.firstChild.node.findChildByType(DartTokenTypes.ABSTRACT)?.psi!!
    }
}


