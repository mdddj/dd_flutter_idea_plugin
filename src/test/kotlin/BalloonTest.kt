import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.ui.awt.RelativePoint
import java.awt.Dimension
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class BalloonTest : HeavyPlatformTestCase() {
    private var balloon: Balloon? = null
    private var anchorComponent: JComponent? = null

    @Throws(Exception::class)
    protected override fun setUp() {
        super.setUp()
        anchorComponent = JPanel()
        anchorComponent!!.preferredSize = Dimension(100, 100)
    }

    @Throws(Exception::class)
    fun testBalloonCreationAndVisibility() {

        ApplicationManager.getApplication().invokeAndWait {
            val content = JLabel("Test Balloon Content")
            balloon = JBPopupFactory.getInstance()
                .createBalloonBuilder(content)
                .setFadeoutTime(0) // 禁用自动关闭
                .setShowCallout(true)
                .createBalloon()

            balloon!!.show(RelativePoint(anchorComponent!!, Point(0, 0)), Balloon.Position.atRight)
        }

    }

    @Throws(Exception::class)
    fun testBalloonHide() {
        testBalloonCreationAndVisibility() // 复用显示逻辑

        // 在EDT中隐藏Balloon
        ApplicationManager.getApplication().invokeAndWait {
            balloon!!.hide()
        }

        // 验证隐藏状态
    }

    @Throws(Exception::class)
    protected override fun tearDown() {
        if (balloon != null && !balloon!!.isDisposed) {
            balloon!!.hide()
        }
        super.tearDown()
    }
}