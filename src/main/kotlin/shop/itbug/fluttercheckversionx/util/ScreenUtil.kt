package shop.itbug.fluttercheckversionx.util

import java.awt.Dimension
import java.awt.Toolkit

class ScreenUtil {


   companion object {
       private fun getScreenSize(): Dimension {
           return Toolkit.getDefaultToolkit().screenSize
       }


       fun getBaseScreenSize(): Dimension{
           val screenSize = getScreenSize()
           return Dimension(screenSize.width - 400, screenSize.height - 400)
       }

   }

}