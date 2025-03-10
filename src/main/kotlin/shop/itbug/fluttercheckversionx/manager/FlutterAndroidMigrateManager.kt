package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.GrIfStatementImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.GrVariableDeclarationImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.GrApplicationStatementImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.path.GrMethodCallExpressionImpl
import shop.itbug.fluttercheckversionx.window.android.AndroidMigrateFile

// flutter android 迁移工具
@Service(Service.Level.PROJECT)
class FlutterAndroidMigrateManager(val project: Project) : Disposable {


    //获取 android/app/build.gradle
    fun findAppBuildFile(): VirtualFile? {
        val androidDir = findAndroidDirectorFile() ?: return null
        return ApplicationManager.getApplication()
            .executeOnPooledThread<VirtualFile?> { androidDir.findFileByRelativePath("app/build.gradle") }.get()
    }

    //获取 android/build.gradle文件
    fun findAndroidBuildFile(): VirtualFile? {
        val androidDirectory = findAndroidDirectorFile() ?: return null
        return ApplicationManager.getApplication()
            .executeOnPooledThread<VirtualFile?> { androidDirectory.findChild("build.gradle") }.get()
    }

    //获取 setting.gradle文件
    fun findSettingsFile(): VirtualFile? {
        val file = findAndroidDirectorFile() ?: return null
        return ApplicationManager.getApplication()
            .executeOnPooledThread<VirtualFile?> { file.findChild("settings.gradle") }.get()
    }


    //获取安卓目录
    fun findAndroidDirectorFile(): VirtualFile? {
        return ApplicationManager.getApplication()
            .executeOnPooledThread<VirtualFile?> { project.guessProjectDir()?.findChild("android") }.get()
    }

    fun getNewSettingsGradleFile(vf: VirtualFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            val psiFile = PsiManager.getInstance(project).findFile(vf) as? GroovyFile ?: return@runWriteCommandAction
            psiFile.fileDocument.setText(newSettingFile.readText())
//            PsiDocumentManager.getInstance(project).commitDocument(psiFile.fileDocument)
        }
    }


    //生成新的 android/build.gradle 文件
    fun getNewAndroidBuildFile(vf: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(vf) as? GroovyFile ?: return
        val methods =
            runReadAction { PsiTreeUtil.findChildrenOfType(psiFile, GrMethodCallExpressionImpl::class.java).toList() }
        val buildscriptElements = runReadAction { methods.filter { it.invokedExpression.text == "buildscript" } }
        ApplicationManager.getApplication().invokeLater({
            WriteCommandAction.runWriteCommandAction(project) {
                buildscriptElements.forEach { it.delete() }
            }
        }, ModalityState.defaultModalityState())
    }

    fun getNewAppBuildFile(vf: VirtualFile) {
        ApplicationManager.getApplication().runWriteAction {
            val psiFile = PsiManager.getInstance(project).findFile(vf) as? GroovyFile ?: return@runWriteAction

            //变量定义
            val varDefines = PsiTreeUtil.findChildrenOfType(psiFile, GrVariableDeclarationImpl::class.java).toList()
            val flutterVar = varDefines.filter {
                it.variables.any { v -> v.name == "flutterRoot" }
            }

            // if
            val ifs = PsiTreeUtil.findChildrenOfType(psiFile, GrIfStatementImpl::class.java).toList().filter {
                it.condition?.text?.contains("flutterRoot") == true
            }

            //插件
            val applyList =
                PsiTreeUtil.findChildrenOfType(psiFile, GrApplicationStatementImpl::class.java).toList().filter {
                    it.firstChild.text == "apply"
                }


            //创建新的
            val insetPlugins = """
                plugins {
                    id "com.android.application"
                    id "kotlin-android"
                    id "dev.flutter.flutter-gradle-plugin"
                }
            """.trimIndent()

            val newPsi = GroovyPsiElementFactory.getInstance(project).createModifierFromText(
                insetPlugins
            )

            WriteCommandAction.writeCommandAction(project).withGlobalUndo().run<Throwable> {
                flutterVar.forEach {
                    it.delete()
                }
                applyList.forEach {
                    it.delete()
                }
                ifs.forEach {
                    it.delete()
                }
                psiFile.addBefore(newPsi, psiFile.firstChild)
            }
        }

    }

    override fun dispose() {

    }

    companion object {

        fun getInstance(project: Project): FlutterAndroidMigrateManager {
            return project.service()
        }

        val FILE = Key.create<AndroidMigrateFile>("FlutterAndroidMigrateManager_FILE")
    }


}


private val i = "includeBuild(\"\$flutterSdkPath/packages/flutter_tools/gradle\")"

private val newSettingFile: VirtualFile
    get() {
        return LightVirtualFile(
            "settings.gradle", FileTypeRegistry.getInstance().getFileTypeByExtension("gradle"), """
                pluginManagement {
                    def flutterSdkPath = {
                        def properties = new Properties()
                        file("local.properties").withInputStream { properties.load(it) }
                        def flutterSdkPath = properties.getProperty("flutter.sdk")
                        assert flutterSdkPath != null, "flutter.sdk not set in local.properties"
                        return flutterSdkPath
                    }()

                    $i

                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }

                plugins {
                    id "dev.flutter.flutter-plugin-loader" version "1.0.0"
                    id "com.android.application" version "8.2.2" apply false
                    id "org.jetbrains.kotlin.android" version "2.1.0" apply false
                }
                
                include ":app"
            """.trimIndent()
        )
    }