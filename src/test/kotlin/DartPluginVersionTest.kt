import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.model.hasNewVersion
import shop.itbug.fluttercheckversionx.model.lastDevVersion
import shop.itbug.fluttercheckversionx.util.ApiService
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.isBeta
import shop.itbug.fluttercheckversionx.util.isDev
import kotlin.test.Test
import kotlin.test.assertEquals

class DartPluginVersionTest {

    @Test
    fun testVersion(): Unit {
        val isarDev = DartPluginVersionName(name = "isar", version = "^4.0.0-dev.14")
        val hiveDev = DartPluginVersionName(name = "hive", version = "4.0.0-dev.2")
        val graphqlBeta = DartPluginVersionName(name = "graphql_flutter", version = "^5.2.0-beta.6")
        val graphqlBeta2 = DartPluginVersionName(name = "graphql_flutter", version = "5.2.0-beta.12")
        val ddJsUtil = DartPluginVersionName(name = "dd_js_util", version = "^3.5.2")
        assertEquals(true, isarDev.isDev())
        assertEquals(true, hiveDev.isDev())
        assertEquals(false, isarDev.isBeta())
        assertEquals(false, hiveDev.isBeta())
        assertEquals(true, graphqlBeta.isBeta())
        assertEquals(false, graphqlBeta.isDev())
        assertEquals(true, graphqlBeta2.isBeta())
        assertEquals(false, graphqlBeta2.isDev())
        assertEquals(false, ddJsUtil.isBeta())
        assertEquals(false, ddJsUtil.isDev())
    }


    ///dev测试
    @Test
    fun lastVersionTest(): Unit {


        /// dev 测试

        val pluginName = "wechat_assets_picker";
        val model: PubVersionDataModel? = ApiService.getPluginDetail(pluginName)
        model?.let {
            assertEquals("8.8.1+1", it.latest.version)
            assertEquals("9.0.0-dev.2", it.lastDevVersion?.version)


            val testVersion = DartPluginVersionName(pluginName, "^9.0.0-dev.2")

            val testVersion2 = DartPluginVersionName(pluginName, "^9.0.0-dev.1")

            val testVersion3 = DartPluginVersionName(pluginName, "9.0.0-dev.1")



            assertEquals(false, it.hasNewVersion(testVersion))
            assertEquals(true, it.hasNewVersion(testVersion2))

            assertEquals("^9.0.0-dev.2", it.getLastVersionText(testVersion2))
            assertEquals("^9.0.0-dev.2", it.getLastVersionText(testVersion3))

        }

    }


    ///基本测试
    @Test
    fun lastVersionTest2(): Unit {


        /// dev 测试

        val pluginName = "dd_js_util";
        val model: PubVersionDataModel? = ApiService.getPluginDetail(pluginName)
        model?.let {
            assertEquals("5.1.6", it.latest.version)
            assertEquals(null, it.lastDevVersion?.version)


            val testVersion = DartPluginVersionName(pluginName, "^9.0.0-dev.2")

            val testVersion2 = DartPluginVersionName(pluginName, "^9.0.0-dev.1")

            val testVersion3 = DartPluginVersionName(pluginName, "5.1.5")



            assertEquals(false, it.hasNewVersion(testVersion))
            assertEquals(false, it.hasNewVersion(testVersion2))

            assertEquals("^5.1.6", it.getLastVersionText(testVersion3))

        }

    }

}