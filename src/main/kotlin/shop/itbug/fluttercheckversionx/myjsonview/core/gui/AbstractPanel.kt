import javax.swing.BorderFactory
import javax.swing.JPanel

open class AbstractPanel : JPanel() {
    init {
        size = maximumSize
        border = BorderFactory.createEmptyBorder(5, 20, 20, 20)
    }

    companion object {
        private const val serialVersionUID = -5139768247575572270L
    }
}