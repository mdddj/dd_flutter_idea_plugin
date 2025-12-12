package shop.itbug.flutterx.cache

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


class YamlFileIgPackageMode : BaseState() {
    var igPackages by map<String, MutableList<String>>()

    //添加一个
    fun addNew(file: PsiFile, name: String) {
        if (hasItem(file, name)) {
            igPackages[file.virtualFile.path]?.remove(file.name)
        }
        val path = file.virtualFile.path
        if (igPackages.contains(path)) {
            igPackages[path]?.add(name)
        } else {
            igPackages[path] = mutableListOf(name)
        }
        incrementModificationCount()
    }

    ///是否已经有项目了
    fun hasItem(file: PsiFile, name: String): Boolean {
        return igPackages[file.virtualFile.path]?.contains(name) == true
    }

    ///移除
    fun remove(file: PsiFile, name: String) {
        val path = file.virtualFile.path
        igPackages[path]?.remove(name)
        incrementModificationCount()
    }

    ///查找全部
    fun findAll(file: PsiFile): List<String> {
        return igPackages[file.virtualFile.path]?.toList() ?: emptyList()
    }
}


@State(name = "DartPackageIgnoreLastVersionUpdate", storages = [Storage("DartPackageIgnoreLastVersionUpdate.xml")])
@Service(Service.Level.PROJECT)
class YamlFileIgDartPackageCache : SimplePersistentStateComponent<YamlFileIgPackageMode>(YamlFileIgPackageMode()) {

    companion object {
        fun getInstance(project: Project): YamlFileIgDartPackageCache =
            project.getService(YamlFileIgDartPackageCache::class.java)
    }
}