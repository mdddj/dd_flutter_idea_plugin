package shop.itbug.flutterx.util

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import java.nio.file.Path

object DartPackageDirectoryUtil {

    fun openInstalledPackageDirectory(project: Project, packageName: String) {
        val task = object : Task.Backgroundable(project, "Opening package directory", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Opening package directory"
                val packageDirectory = findInstalledPackageDirectory(project, packageName) ?: return
                BrowserUtil.browse(Path.of(packageDirectory.path))
            }
        }
        task.queue()
    }

    private fun findInstalledPackageDirectory(project: Project, packageName: String): VirtualFile? {
        val roots = ProjectRootManager.getInstance(project).orderEntries().roots(OrderRootType.CLASSES).roots
        return roots.firstOrNull { root ->
            if (!root.isDirectory) {
                return@firstOrNull false
            }
            runReadAction { readPackageName(project, root) == packageName }
        }?.let { root ->
            if (root.name == "lib") root.parent else root
        }
    }

    private fun readPackageName(project: Project, root: VirtualFile): String? {
        val packageRoot = (if (root.name == "lib") root.parent else root) ?: return null
        val pubspecFile = packageRoot.findChild("pubspec.yaml") ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(pubspecFile) as? YAMLFile ?: return null
        return YAMLUtil.getQualifiedKeyInFile(psiFile, "name")?.valueText
    }
}
