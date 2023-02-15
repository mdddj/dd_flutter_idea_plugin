package shop.itbug.fluttercheckversionx.actions

import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.dialog.AssetsAutoGenerateClassActionConfigDialog
import shop.itbug.fluttercheckversionx.util.*
import java.io.File


/// 自动正常资产文件调用
///
class AssetsAutoGenerateClassAction : AnAction() {


    private val names: MutableList<String> = mutableListOf()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)

        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)!!
        val name = vf.name

        project?.apply {
            val userSetting = GenerateAssetsClassConfig.getGenerateAssetsSetting()

            val isOk =
                if (userSetting.dontTip) true else AssetsAutoGenerateClassActionConfigDialog(project).showAndGet()
            if (!isOk) {
                return
            }


            val classElement = MyDartPsiElementUtil.createDartClassBodyFromClassName(project, userSetting.className)

            classElement.classBody?.classMembers?.let { classMembers ->
                MyFileUtil.onFolderEachWithProject(project, name) { virtualFile ->
                    val attrName = formatName(virtualFile, userSetting, project, names)//属性名
                    if (attrName.isNotEmpty()) {
                        val eleValue = virtualFile.fileNameWith(name)//属性值
                        val expression = "static const $attrName = '$eleValue'"
                        names.add(attrName)
                        MyDartPsiElementUtil.createVarExpressionFromText(
                            project,
                            expression
                        ).let {
                            val d = MyDartPsiElementUtil.createLeafPsiElement(project)
                            runWriteAction {
                                it?.addAfter(d, it.nextSibling)
                                it?.let { it1 -> classMembers.addAfter(it1, classMembers.nextSibling) }
                            }
                        }
                    }
                }
            }

            project.reformat(classElement)

            MyDartPsiElementUtil.createDartFileWithElement(
                project,
                classElement,
                "lib",
                "${userSetting.fileName}.dart",
                null
            )
        }
        names.clear()
    }


    /**
     * 格式化属性名
     */
    private fun formatName(
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


    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf != null && vf.isDirectory
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}