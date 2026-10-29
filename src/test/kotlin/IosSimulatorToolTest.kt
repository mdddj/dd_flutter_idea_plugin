import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import shop.itbug.flutterx.tools.IosSimulatorTool

class IosSimulatorToolTest {

    @Test
    fun `parses available iOS simulators and skips other runtimes`() {
        val json = """
            {
              "devices": {
                "com.apple.CoreSimulator.SimRuntime.iOS-18-2": [
                  { "udid": "AAAA", "name": "iPhone 17 Pro", "state": "Booted", "isAvailable": true },
                  { "udid": "BBBB", "name": "iPhone 16", "state": "Shutdown", "isAvailable": true },
                  { "udid": "CCCC", "name": "Broken iPhone", "state": "Shutdown", "isAvailable": false }
                ],
                "com.apple.CoreSimulator.SimRuntime.iOS-17-5": [
                  { "udid": "DDDD", "name": "iPhone 15", "state": "Shutdown", "isAvailable": true }
                ],
                "com.apple.CoreSimulator.SimRuntime.watchOS-11-0": [
                  { "udid": "EEEE", "name": "Apple Watch", "state": "Shutdown", "isAvailable": true }
                ]
              }
            }
        """.trimIndent()

        val simulators = IosSimulatorTool.parseSimulators(json)

        assertEquals(3, simulators.size)
        // iOS 版本倒序，同版本按名称排序
        assertEquals(listOf("iPhone 16", "iPhone 17 Pro", "iPhone 15"), simulators.map { it.name })
        assertEquals("iOS 18.2", simulators[0].runtime)
        assertEquals("iOS 17.5", simulators[2].runtime)
        assertFalse(simulators[0].isBooted)
        assertTrue(simulators[1].isBooted)
        assertFalse(simulators.any { it.name == "Apple Watch" })
        assertFalse(simulators.any { it.udid == "CCCC" })
    }

    @Test
    fun `returns empty list for malformed output`() {
        assertTrue(IosSimulatorTool.parseSimulators("xcrun: error: unable to find utility").isEmpty())
        assertTrue(IosSimulatorTool.parseSimulators("{}").isEmpty())
        assertTrue(IosSimulatorTool.parseSimulators("""{"devices": {"com.apple.CoreSimulator.SimRuntime.iOS": []}}""").isEmpty())
    }
}
