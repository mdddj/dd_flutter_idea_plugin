import com.intellij.openapi.diagnostic.ErrorReportSinkBean
import com.intellij.openapi.diagnostic.UnhandledExceptionReport
import com.intellij.openapi.diagnostic.UnhandledFreezeReport
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.sentry.SentryLevel
import kotlinx.coroutines.runBlocking
import shop.itbug.flutterx.common.FlutterXErrorReportSink
import shop.itbug.flutterx.common.SentryReportingService
import shop.itbug.flutterx.config.FlutterXGlobalConfigService
import shop.itbug.flutterx.constance.SENTRY_DSN

/**
 * Sentry 上报链路：EP 注册、DSN 脱敏、事件载荷、开关行为。
 *
 * 不打真实网络：这里验证从平台报告到 `SentryEvent` 的转换和关闭态行为；
 * 真实投递由设置页的"发送测试事件"覆盖。
 */
class SentryReportingServiceTest : BasePlatformTestCase() {

    fun testSinkIsRegistered() {
        // errorReportSink 用 beanClass 注册，EP 列表里是 bean 而不是 sink 实例
        val beans = ErrorReportSinkBean.EP_NAME.extensionList
            .filter { it.implementation == "shop.itbug.flutterx.common.FlutterXErrorReportSink" }

        assertEquals(1, beans.size)
        assertEquals("shop.itbug.FlutterCheckVersionX", beans.single().pluginDescriptor.pluginId.idString)
        assertTrue(beans.single().instance is FlutterXErrorReportSink)
    }

    fun testNoInternalApiExtensionPointsAreUsed() {
        // 平台把 applicationInitializedListener 整个接口标成 @ApiStatus.Internal，插件不许注册
        val pluginXml = javaClass.classLoader
            .getResourceAsStream("META-INF/plugin.xml")!!
            .bufferedReader()
            .use { it.readText() }

        assertFalse(
            "applicationInitializedListener is @ApiStatus.Internal",
            pluginXml.contains("applicationInitializedListener")
        )
        assertFalse(
            "ApplicationInitializedListener is @ApiStatus.Internal",
            pluginXml.contains("ApplicationInitializedListener")
        )
    }

    fun testDefaultDsnUsesHttps() {
        assertTrue("must be HTTPS", SENTRY_DSN.startsWith("https://"))
        assertTrue(SENTRY_DSN.endsWith("/12"))
        assertTrue(SENTRY_DSN.substringAfter("://").substringBefore("@").isNotEmpty())
    }

    fun testLegacyPlaintextDsnIsMigratedOnLoad() {
        // 旧版存的是明文 http + 同一个公钥；这里用公钥拼一个旧形态的 DSN
        val key = SENTRY_DSN.substringAfter("://").substringBefore("@")
        val service = FlutterXGlobalConfigService()
        val state = FlutterXGlobalConfigService.MyState()
        state.sentryDsn = "http://$key@legacy.invalid/12"

        service.loadState(state)

        assertEquals(SENTRY_DSN, state.sentryDsn)
    }

    fun testCustomDsnIsNotOverwrittenByMigration() {
        val service = FlutterXGlobalConfigService()
        val state = FlutterXGlobalConfigService.MyState()
        state.sentryDsn = "https://custom@example.com/9"

        service.loadState(state)

        assertEquals("https://custom@example.com/9", state.sentryDsn)
    }

    fun testForeignPlaintextDsnIsNotMigrated() {
        // 明文但不是本插件的公钥，不能擅自改写
        val service = FlutterXGlobalConfigService()
        val state = FlutterXGlobalConfigService.MyState()
        state.sentryDsn = "http://someoneelse@example.com/3"

        service.loadState(state)

        assertEquals("http://someoneelse@example.com/3", state.sentryDsn)
    }

    fun testMaskDsnKeepsKeySecret() {
        val masked = SentryReportingService.maskDsn(SENTRY_DSN)

        assertFalse("public key must not be logged", masked.contains("436a5096cc2c4f38b8285d6026541b03"))
        assertTrue(masked.contains("rustrak.itbug.shop/12"))
        assertEquals("(empty)", SentryReportingService.maskDsn(""))
        assertEquals("***", SentryReportingService.maskDsn("no-dsn-here"))
    }

    fun testDisabledConfigSendsNothing() {
        val state = FlutterXGlobalConfigService.getInstance().state
        val previous = state.sentryEnabled
        state.sentryEnabled = false
        val service = SentryReportingService()
        try {
            // 惰性初始化会真的 init SDK，但配置关闭时 setEnabled(false)，因此一条都不发
            assertEquals(null, service.captureError(IllegalStateException("boom")))
            assertEquals(null, service.captureFreeze(1234, "frozen"))
            assertEquals(null, service.sendTestEvent())
            assertFalse(service.isReporting)
        } finally {
            service.flushAndClose(flushMillis = 100)
            state.sentryEnabled = previous
        }
    }

    fun testConfigurationIsAppliedToTheProvidedOptionsInstance() {
        // Sentry.init 的 lambda 重载要求就地配置传入的实例。若误 return 新对象，
        // 配置会被丢弃，DSN 为空直接抛 IllegalArgumentException——这条守住回归。
        val service = SentryReportingService()
        val options = io.sentry.SentryOptions()

        val configured = service.applyConfiguration(options, SENTRY_DSN, enabled = true, debug = false)

        assertSame("must configure the caller's instance", options, configured)
        assertEquals(SENTRY_DSN, options.dsn)
        assertEquals("FlutterX@7.3.2", options.release)
        assertTrue(options.isDebug == false)
        assertFalse("platform owns the uncaught handler", options.isEnableUncaughtExceptionHandler)
        assertFalse("platform owns the shutdown hook", options.isEnableShutdownHook)
        assertFalse("sentry.properties must not hijack config", options.isEnableExternalConfiguration)
    }

    fun testDisabledConfigurationInitializesWithoutDsn() {
        val service = SentryReportingService()
        val options = io.sentry.SentryOptions()

        service.applyConfiguration(options, dsn = "", enabled = false, debug = false)

        assertFalse("empty DSN must disable the SDK instead of throwing", options.isEnabled)
    }

    fun testEmptyDsnSendsNothingAndDoesNotThrow() {
        val state = FlutterXGlobalConfigService.getInstance().state
        val previousDsn = state.sentryDsn
        val previousEnabled = state.sentryEnabled
        state.sentryEnabled = true
        state.sentryDsn = ""
        val service = SentryReportingService()
        try {
            // 空 DSN 会让 SDK 抛 IllegalArgumentException，必须被吞掉而不是崩
            assertEquals(null, service.captureError(IllegalStateException("boom")))
            assertFalse(service.isReporting)
        } finally {
            service.flushAndClose(flushMillis = 100)
            state.sentryDsn = previousDsn
            state.sentryEnabled = previousEnabled
        }
    }

    fun testErrorEventCarriesExceptionFramesAndTags() {
        val service = SentryReportingService()
        val throwable = IllegalStateException("boom").apply { stackTrace = Thread.currentThread().stackTrace }

        val event = service.buildErrorEvent(throwable, "custom message", tags = mapOf("report.kind" to "test"))

        assertNotNull(event)
        assertEquals(SentryLevel.ERROR, event!!.level)
        assertEquals("custom message", event.message!!.message)
        val exception = event.exceptions!!.single()
        assertEquals("java.lang.IllegalStateException", exception.type)
        assertEquals("boom", exception.value)
        assertTrue("frames must be attached", exception.stacktrace!!.frames!!.isNotEmpty())
        assertEquals(true, exception.mechanism!!.isHandled)
        assertEquals("test", event.tags?.get("report.kind"))
        // 插件身份必须随事件走，否则 Sentry 里分不清版本
        assertEquals("7.3.2", event.tags?.get("plugin.version"))
        assertNotNull(event.tags?.get("ide"))
        assertNotNull(event.tags?.get("os"))
    }

    fun testErrorEventFallsBackToThrowableMessage() {
        val service = SentryReportingService()

        val event = service.buildErrorEvent(IllegalStateException("only message"), null)

        assertEquals("only message", event!!.message!!.message)
    }

    fun testErrorEventIsNullWithoutContent() {
        val service = SentryReportingService()

        assertEquals(null, service.buildErrorEvent(null, null))
        assertEquals(null, service.buildErrorEvent(null, "   "))
    }

    fun testPluginFramesAreMarkedInApp() {
        val service = SentryReportingService()
        val throwable = IllegalStateException("boom").apply {
            stackTrace = arrayOf(
                StackTraceElement("shop.itbug.flutterx.Foo", "bar", "Foo.kt", 12),
                StackTraceElement("com.other.Lib", "baz", "Lib.kt", 40),
            )
        }

        val frames = service.buildErrorEvent(throwable, null)!!.exceptions!!.single().stacktrace!!.frames!!

        assertEquals(true, frames[0].isInApp)
        assertEquals(false, frames[1].isInApp)
        assertEquals(12, frames[0].lineno)
        assertEquals("Foo.kt", frames[0].filename)
    }

    fun testFreezeEventCarriesDuration() {
        val service = SentryReportingService()

        val event = service.buildFreezeEvent(4321, "EDT frozen")

        assertEquals(SentryLevel.WARNING, event.level)
        assertEquals("UI freeze 4321ms: EDT frozen", event.message!!.message)
        assertEquals("freeze", event.tags?.get("report.kind"))
        assertEquals(4321L, event.extras?.get("freeze.duration.ms"))
    }

    fun testSinkDropsReportsWhenReportingDisabled() {
        val state = FlutterXGlobalConfigService.getInstance().state
        val previous = state.sentryEnabled
        state.sentryEnabled = false
        val sink = FlutterXErrorReportSink()
        try {
            // 平台可能在任意时刻推送报告；关闭上报时必须安静丢弃
            runBlocking { sink.submit(UnhandledExceptionReport(IllegalStateException("early"))) }
            runBlocking { sink.submit(UnhandledFreezeReport("EDT frozen", 4321L, emptyList(), emptyList())) }
        } finally {
            state.sentryEnabled = previous
        }
    }
}
