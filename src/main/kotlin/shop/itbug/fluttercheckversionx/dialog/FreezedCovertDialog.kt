package shop.itbug.fluttercheckversionx.dialog

import com.alibaba.fastjson2.toJSONString
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import javax.swing.JComponent

class FreezedCovertDialog(val project: Project, val model: FreezedCovertModel) : DialogWrapper(project) {
    init {
        super.init()
        title = "模型转Freezed对象(${model.className})"
        println(model.toJSONString())
        setSize(500, 380)

    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                button("生成") {
                    generateFreezedModel()
                }
            }
        }
    }


    /**
     * 生成freezed类
     */
    private fun generateFreezedModel(){

    }

}