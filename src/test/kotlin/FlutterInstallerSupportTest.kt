package shop.itbug.flutterx.dialog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FlutterInstallerSupportTest {
    @Test
    fun `parses supported Android and iOS devices`() {
        val output = """
            Flutter tool startup message
            [
              {
                "name": "Android Phone",
                "id": "android-1",
                "isSupported": true,
                "targetPlatform": "android-arm64",
                "emulator": false
              },
              {
                "name": "iPhone Simulator",
                "id": "ios-simulator-1",
                "isSupported": true,
                "targetPlatform": "ios",
                "emulator": true
              },
              {
                "name": "Unsupported iPhone",
                "id": "ios-unsupported",
                "isSupported": false,
                "targetPlatform": "ios"
              },
              {
                "name": "Chrome",
                "id": "chrome",
                "isSupported": true,
                "targetPlatform": "web-javascript"
              }
            ]
        """.trimIndent()

        val devices = FlutterInstallerSupport.parseDevices(output)

        assertEquals(2, devices.size)
        assertEquals(FlutterArtifactPlatform.ANDROID, devices[0].platform)
        assertFalse(devices[0].isSimulator)
        assertEquals(FlutterArtifactPlatform.IOS, devices[1].platform)
        assertTrue(devices[1].isSimulator)
    }

    @Test
    fun `scans installable Flutter artifacts and skips generated caches`() {
        val root = Files.createTempDirectory("flutter-installer-test")
        try {
            val apk = Files.createDirectories(root.resolve("build/app/outputs/flutter-apk"))
                .resolve("app-release.apk")
            Files.createFile(apk)
            val ipa = Files.createDirectories(root.resolve("build/ios/ipa"))
                .resolve("Runner.ipa")
            Files.createFile(ipa)
            Files.createDirectories(root.resolve("build/ios/iphonesimulator/Runner.app"))
            Files.createDirectories(root.resolve("build/macos/Build/Products/Release/Runner.app"))
            val ignored = Files.createDirectories(root.resolve(".dart_tool/cache"))
                .resolve("ignored.apk")
            Files.createFile(ignored)

            val artifacts = FlutterInstallerSupport.scanArtifacts(root)

            assertEquals(3, artifacts.size)
            assertTrue(artifacts.any { it.path == apk && it.platform == FlutterArtifactPlatform.ANDROID })
            assertTrue(artifacts.any { it.path == ipa && it.platform == FlutterArtifactPlatform.IOS })
            assertTrue(artifacts.any { it.isAppBundle && it.path.fileName.toString() == "Runner.app" })
            assertFalse(artifacts.any { it.path == ignored })
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
