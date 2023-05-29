package shop.itbug.fluttercheckversionx.util

///扫描未使用的包
object ScanPackageUtil {


//    fun doScan(project: Project) {
//        val files = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME,DartFileType.INSTANCE,
//            GlobalSearchScope.projectScope(project))
//        val instance = PsiManager.getInstance(project)
//
//        files.forEach {
//            if(it.fileType is DartFileType){
//                val findFile = instance.findFile(it)!!
//                val impls =  PsiTreeUtil.findChildrenOfAnyType(findFile, DartUriElementBase::class.java)
//
//                impls.forEach { ele ->
//                    val  eleText =  ele.text.replace("\"","").replace("'","")
//                    val importedFile = DartResolveUtil.getImportedFile(project, it, eleText)
//                    if(importedFile!=null){
//                        val partUris = DartAnalysisServerService.getInstance(project).analysis_getHover(it,ele.startOffset)
//                        println("路径:${importedFile.path}")
//                        val analysisGethover =
//                            DartAnalysisServerService.getInstance(project).analysis_getHover(importedFile, 0)
//                        println(analysisGethover)
//                    }
//                }
//            }
//
//        }
//
//    }

}
