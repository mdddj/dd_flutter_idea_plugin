// 请根据你的插件包名修改

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel

class MyPopupTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        // 如果你需要测试数据文件，请在此处指定路径
        return "src/test/testData"
    }

    fun testPopupDisplay() {
        // 1. 准备环境（例如，打开文件，设置编辑器内容）
        //    如果你的Popup依赖于特定的文件内容或光标位置，请在此处设置。
        myFixture.configureByText("dummy.txt", "")
        //    myFixture.editor.caretModel.moveToOffset(0)

        // 2. 触发显示 Popup 的动作
        //    这部分需要根据你的Popup实际触发方式进行修改。
        //    以下是一个使用 JBPopupFactory 创建简单文本Popup的示例。
        //    如果你是通过 AnAction 触发的，你需要模拟 AnAction 的执行。
        //    val myAction = AnAction.Factory.getInstance().getAction("your.action.id")
        //    myFixture.testAction(myAction)

        var createdPopup: JBPopup? = null
        ApplicationManager.getApplication().invokeAndWait {
            // 替换为你的实际 Popup 创建和显示逻辑
            // 例如：如果你有一个 MyPopupCreator.showMyPopup(project, editor) 方法
            // MyPopupCreator.showMyPopup(myFixture.project, myFixture.editor)

            // 示例：创建一个简单的HTML文本Popup
            createdPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(JBLabel("hello"),null)
                .createPopup()
            createdPopup.showInBestPositionFor(myFixture.editor)
            Thread.sleep(500) // 增加延迟到 500ms
        }

        // 3. 断言 Popup 的存在和可见性
        assertNotNull("Popup should be displayed", createdPopup)
        assertTrue("Popup should be visible", createdPopup!!.isVisible)

        // 4. 断言 Popup 的内容 (这部分高度依赖于你的Popup的实际内容组件)
        //    如果你的Popup内容是简单的文本，你可以尝试获取其内容组件并断言文本。
        //    如果你的Popup内容更复杂，你可能需要遍历组件树来查找特定的子组件。
        val contentComponent = createdPopup!!.content
        // 示例：如果Popup内容是一个JBLabel
        // assertTrue(contentComponent is JBLabel)
        // assertEquals("Hello, Popup!", (contentComponent as JBLabel).text)

        // 5. 如果需要，关闭 Popup
        ApplicationManager.getApplication().invokeAndWait {
            createdPopup?.cancel()
        }
        assertFalse("Popup should be closed after cancellation", createdPopup?.isVisible ?: true)
    }
}
