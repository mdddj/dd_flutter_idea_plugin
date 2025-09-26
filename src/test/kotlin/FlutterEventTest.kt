import org.junit.Test
import shop.itbug.fluttercheckversionx.common.dart.FlutterEventFactory

class FlutterEventTest {
    
    @Test
    fun testParseArrayFormat() {
        val jsonText = """[{"event":"app.debugPort","params":{"appId":"c1493070-5955-4058-a23c-79f0c52a9c31","port":53417,"wsUri":"ws://127.0.0.1:53417/kNzF6uEFtck=/ws","baseUri":"file:///Users/ldd/Library/Containers/com.example.flutterdemo/Data/tmp/flutterdemoR4GNFc/flutterdemo/"}}]"""
        
        val event = FlutterEventFactory.formJsonText(jsonText)
        
        assert(event != null)
        assert(event!!.event == "app.debugPort")
        assert(event.params?.appId == "c1493070-5955-4058-a23c-79f0c52a9c31")
        assert(event.params?.wsUri == "ws://127.0.0.1:53417/kNzF6uEFtck=/ws")
        
        println("✅ 数组格式解析测试通过")
    }
    
    @Test
    fun testParseObjectFormat() {
        val jsonText = """{"event":"app.start","params":{"appId":"test-id","deviceId":"macos","mode":"debug"}}"""
        
        val event = FlutterEventFactory.formJsonText(jsonText)
        
        assert(event != null)
        assert(event!!.event == "app.start")
        assert(event.params?.appId == "test-id")
        assert(event.params?.deviceId == "macos")
        
        println("✅ 对象格式解析测试通过")
    }
}