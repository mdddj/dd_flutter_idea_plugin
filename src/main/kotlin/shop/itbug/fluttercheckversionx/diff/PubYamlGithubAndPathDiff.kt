package shop.itbug.fluttercheckversionx.diff

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest

class PubYamlGithubAndPathDiff: DiffExtension() {
    override fun onViewerCreated(viewer: FrameDiffTool.DiffViewer, context: DiffContext, request: DiffRequest) {
        println("PubYamlGithubAndPathDiff onViewerCreated.")
    }
}