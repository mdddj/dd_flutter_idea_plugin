package shop.itbug.flutterx.common

import codegen.FlutterXPluginInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.system.OS
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Mechanism
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import io.sentry.protocol.SentryStackFrame
import io.sentry.protocol.SentryStackTrace
import shop.itbug.flutterx.config.FlutterXGlobalConfigService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** 错误上报相关通知使用的分组，需与 plugin.xml 的 notificationGroup id 一致。 */
const val SENTRY_NOTIFICATION_GROUP = "flutterx_error_report"

/**
 * 把插件相关的未处理异常和 UI 冻结事件自动上报到自建 Sentry。
 *
 * 数据流：IDE 平台把归属本插件的报告交给 [FlutterXErrorReportSink] → 本服务 → Sentry。
 * 用户不点任何按钮也会上报；`Settings → FlutterX` 里可以整体关闭或改 DSN。
 * 注意：这和 [PluginErrorHandle]（手动开 GitHub issue）是两条独立通道。
 *
 * Sentry 的全局 hub 是静态状态，因此只在应用启动时 init 一次，退出时 flush 后 close。
 */
@Service(Service.Level.APP)
class SentryReportingService : Disposable {

    private val logger = Logger.getInstance(SentryReportingService::class.java)
    private val started = AtomicBoolean(false)
    private val sentCount = AtomicInteger(0)
    private val lastSentAt = ConcurrentHashMap<String, Long>()
    private val startLock = Any()

    /** 是否已初始化并且真的在发送。纯读取，不触发初始化。 */
    val isReporting: Boolean
        get() = started.get() && Sentry.isEnabled()

    /**
     * 启动 Sentry，重复调用无副作用。
     *
     * 只在第一次需要发送时调用（惰性初始化）。这样无需任何启动钩子：
     * 平台的 `applicationInitializedListener` 标了 `@ApiStatus.Internal`，插件不许用。
     * 配置关闭时用 `enabled = false` 初始化，因为空 DSN 会抛 IllegalArgumentException。
     */
    fun start() {
        ensureStarted()
    }

    /** 按需初始化；返回是否真的可以向 Sentry 发送。 */
    private fun ensureReporting(): Boolean = ensureStarted() && Sentry.isEnabled()

    /**
     * 按需初始化，成功与否只尝试一次。
     * 返回 true 表示 `Sentry.init` 已执行（是否真的发送还要看 [Sentry.isEnabled]）。
     */
    private fun ensureStarted(): Boolean {
        if (started.get()) return true
        synchronized(startLock) {
            if (started.get()) return true
            val config = FlutterXGlobalConfigService.getInstance().state
            val dsn = config.sentryDsn.orEmpty().trim()
            val enabled = config.sentryEnabled && dsn.isNotEmpty()
            return try {
                // 用 lambda 重载：Sentry.init(SentryOptions) 在 8.x 里标了 @ApiStatus.Internal。
                // 必须配置传入的 options 实例，不能 return 一个新对象。
                Sentry.init { options -> applyConfiguration(options, dsn, enabled, config.sentryDebug) }
                logger.info("Sentry reporting started: enabled=$enabled dsn=${maskDsn(dsn)}")
                started.set(true)
                true
            } catch (e: Throwable) {
                // 上报组件自身的故障绝不能影响 IDE
                logger.warn("Failed to initialize Sentry reporting", e)
                false
            }
        }
    }

    /**
     * 发送一条异常。返回 Sentry 事件 id；被限流、去重或未启用时返回 null。
     */
    fun captureError(
        throwable: Throwable?,
        message: String? = null,
        level: SentryLevel = SentryLevel.ERROR,
        tags: Map<String, String> = emptyMap(),
        attachments: Collection<Attachment> = emptyList(),
        threadDumps: Collection<String> = emptyList(),
    ): String? {
        if (!ensureReporting()) return null
        val event = buildErrorEvent(throwable, message, level, tags) ?: return null
        val text = message?.takeIf { it.isNotBlank() } ?: throwable?.message
        val dedupeKey = throwable?.let { it.javaClass.name + "|" + it.stackTrace.firstOrNull() } ?: text
        return send(event, dedupeKey, attachments, threadDumps)
    }

    /**
     * 发送一条 UI 冻结事件。冻结本身没有异常，用带时长的事件 + 线程转储附件表示。
     */
    fun captureFreeze(
        durationMs: Long,
        reason: String?,
        attachments: Collection<Attachment> = emptyList(),
        threadDumps: Collection<String> = emptyList(),
    ): String? {
        if (!ensureReporting()) return null
        val event = buildFreezeEvent(durationMs, reason)
        return send(event, "freeze|${durationMs}|${reason.orEmpty()}", attachments, threadDumps)
    }

    /**
     * 构造异常事件。返回 null 表示这条报告没有可发送的内容。
     * 与发送分离，方便单元测试不联网地检查载荷。
     */
    internal fun buildErrorEvent(
        throwable: Throwable?,
        message: String?,
        level: SentryLevel = SentryLevel.ERROR,
        tags: Map<String, String> = emptyMap(),
    ): SentryEvent? {
        val text = message?.takeIf { it.isNotBlank() } ?: throwable?.message?.takeIf { it.isNotBlank() }
        if (throwable == null && text == null) return null
        return SentryEvent().apply {
            this.level = level
            if (text != null) {
                this.message = Message().apply {
                    this.message = text
                    this.formatted = text
                }
            }
            if (throwable != null) exceptions = listOf(throwable.toSentryException())
            tags.forEach { (key, value) -> setTag(key, value) }
            applyPluginTags()
        }
    }

    /** 构造 UI 冻结事件。冻结没有异常，只带时长和线程转储附件。 */
    internal fun buildFreezeEvent(durationMs: Long, reason: String?): SentryEvent {
        val text = "UI freeze ${durationMs}ms" + (reason?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
        return SentryEvent().apply {
            level = SentryLevel.WARNING
            message = Message().apply {
                this.message = text
                this.formatted = text
            }
            setTag("report.kind", "freeze")
            setExtra("freeze.duration.ms", durationMs)
            applyPluginTags()
        }
    }

    /** 发一条测试事件，用于在设置页确认 DSN 可用。 */
    fun sendTestEvent(): String? = captureError(
        throwable = IllegalStateException("FlutterX Sentry test event"),
        message = "FlutterX Sentry test event",
        level = SentryLevel.INFO,
        tags = mapOf("report.kind" to "test"),
    )

    /**
     * 退出前把队列刷完。IDE 关闭时调用，最多等 [flushMillis]。
     * 之后的 `Sentry.captureEvent` 会变成空操作，直到下一次需要发送时重新惰性初始化。
     */
    fun flushAndClose(flushMillis: Long = FLUSH_TIMEOUT_MS) {
        if (!started.get()) return
        runCatching {
            if (Sentry.isEnabled()) Sentry.flush(flushMillis)
        }.onFailure { logger.warn("Sentry flush failed", it) }
        runCatching { Sentry.close() }.onFailure { logger.warn("Sentry close failed", it) }
        started.set(false)
    }

    private fun send(
        event: SentryEvent,
        dedupeKey: String?,
        attachments: Collection<Attachment>,
        threadDumps: Collection<String>,
    ): String? {
        if (sentCount.get() >= MAX_EVENTS_PER_SESSION) return null
        if (dedupeKey != null && isDuplicate(dedupeKey)) return null
        val hint = buildHint(attachments, threadDumps)
        return runCatching {
            val id = Sentry.captureEvent(event, hint).toString()
            if (id.isBlank()) return null
            sentCount.incrementAndGet()
            logger.info("Sentry event sent: $id")
            id
        }.onFailure { logger.warn("Sentry capture failed", it) }.getOrNull()
    }

    private fun buildHint(attachments: Collection<Attachment>, threadDumps: Collection<String>): Hint {
        val hint = Hint()
        attachments.forEach { attachment ->
            val bytes = runCatching { attachment.bytes }.getOrNull() ?: return@forEach
            if (bytes.isEmpty()) return@forEach
            hint.addAttachment(io.sentry.Attachment(bytes, attachment.name.ifBlank { "attachment.txt" }))
        }
        threadDumps.forEachIndexed { index, dump ->
            if (dump.isBlank()) return@forEachIndexed
            val bytes = dump.toByteArray()
            hint.addAttachment(io.sentry.Attachment(bytes, "thread-dump-${index + 1}.txt"))
        }
        return hint
    }

    private fun isDuplicate(key: String): Boolean {
        val now = System.nanoTime()
        val previous = lastSentAt[key]
        if (previous != null && now - previous < DEDUPE_WINDOW_NANOS) return true
        lastSentAt[key] = now
        // 防止无限增长：超出上限直接清空，最坏情况只是丢弃去重记忆
        if (lastSentAt.size > MAX_DEDUPE_KEYS) lastSentAt.clear()
        return false
    }

    internal fun applyConfiguration(options: SentryOptions, dsn: String, enabled: Boolean, debug: Boolean): SentryOptions = options.apply {
        if (enabled) setDsn(dsn) else setEnabled(false)
        // 平台自己管未捕获异常和关闭流程，Sentry 不要插手
        setEnableUncaughtExceptionHandler(false)
        setEnableShutdownHook(false)
        setEnableExternalConfiguration(false)
        setAttachServerName(false)
        setEnableAutoSessionTracking(false)
        setMaxBreadcrumbs(0)
        setRelease("${FlutterXPluginInfo.NAME}@${FlutterXPluginInfo.VERSION}")
        setDist(FlutterXPluginInfo.VERSION)
        setEnvironment(if (ApplicationInfo.getInstance().isEAP) "eap" else "production")
        setDebug(debug)
        setDiagnosticLevel(if (debug) SentryLevel.DEBUG else SentryLevel.WARNING)
        setConnectionTimeoutMillis(CONNECT_TIMEOUT_MS)
        setReadTimeoutMillis(READ_TIMEOUT_MS)
        setShutdownTimeoutMillis(FLUSH_TIMEOUT_MS)
        setFlushTimeoutMillis(FLUSH_TIMEOUT_MS)
        // 网络失败的事件落到系统目录，下次启动重试，最多保留 MAX_CACHE_ITEMS 条
        setCacheDirPath(File(PathManager.getSystemPath(), "flutterx-sentry").absolutePath)
        setMaxCacheItems(MAX_CACHE_ITEMS)
        setServerName(null)
        setLogger(IdeaSentryLogger(debug))
    }

    private fun SentryEvent.applyPluginTags() {
        val info = ApplicationInfo.getInstance()
        setTag("plugin.version", FlutterXPluginInfo.VERSION)
        setTag("ide", info.build.asString())
        setTag("ide.product", info.build.productCode)
        setTag("os", "${OS.CURRENT.name} ${OS.CURRENT.version()}")
        setTag("java", System.getProperty("java.version") ?: "unknown")
    }

    private fun Throwable.toSentryException(): SentryException {
        val frames = (stackTrace ?: emptyArray()).map { element ->
            SentryStackFrame().apply {
                module = element.className.substringBeforeLast('.', "")
                setInApp(element.className.startsWith("shop.itbug."))
                function = element.methodName
                filename = element.fileName
                lineno = element.lineNumber.takeIf { it > 0 }
                platform = "java"
            }
        }
        return SentryException().apply {
            type = this@toSentryException.javaClass.name
            value = this@toSentryException.message ?: this@toSentryException.javaClass.simpleName
            module = this@toSentryException.javaClass.name.substringBeforeLast('.', "")
            stacktrace = SentryStackTrace().apply { this.frames = frames }
            mechanism = Mechanism().apply {
                type = "generic"
                setHandled(true)
            }
        }
    }

    override fun dispose() {
        flushAndClose()
    }

    companion object {
        private const val FLUSH_TIMEOUT_MS = 5000L
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 5000
        private const val MAX_EVENTS_PER_SESSION = 200
        private const val MAX_CACHE_ITEMS = 20
        private const val MAX_DEDUPE_KEYS = 500
        private const val DEDUPE_WINDOW_NANOS = 60_000_000_000L

        fun getInstance(): SentryReportingService = service()

        /** 日志里不要把完整 DSN（含 key）打出来。 */
        internal fun maskDsn(dsn: String): String {
            if (dsn.isEmpty()) return "(empty)"
            val at = dsn.lastIndexOf('@')
            if (at < 0) return "***"
            val schemeEnd = dsn.indexOf("://")
            val scheme = if (schemeEnd > 0) dsn.substring(0, schemeEnd + 3) else ""
            return "$scheme***${dsn.substring(at)}"
        }
    }
}

/**
 * 把 Sentry SDK 自己的日志转到 IDE 日志。
 *
 * Sentry 默认打印到 stderr，插件里会把 IDEA 的 console 弄乱；这里按级别过滤后写 idea.log。
 */
internal class IdeaSentryLogger(private val debug: Boolean) : io.sentry.ILogger {

    private val logger = Logger.getInstance(SentryReportingService::class.java)

    override fun log(level: SentryLevel, message: String, vararg args: Any?) {
        emit(level, null, format(message, args))
    }

    override fun log(level: SentryLevel, message: String, throwable: Throwable?) {
        emit(level, throwable, message)
    }

    override fun log(level: SentryLevel, throwable: Throwable?, message: String, vararg args: Any?) {
        emit(level, throwable, format(message, args))
    }

    /** Sentry 用 `%s` 占位符，直接打印会丢掉参数；这里手动替换。 */
    private fun format(message: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return message
        val builder = StringBuilder(message.length + 32)
        var argIndex = 0
        var index = 0
        while (index < message.length) {
            val char = message[index]
            if (char == '%' && index + 1 < message.length && argIndex < args.size) {
                when (message[index + 1]) {
                    's' -> {
                        builder.append(args[argIndex++])
                        index += 2
                        continue
                    }

                    'd' -> {
                        builder.append(args[argIndex++])
                        index += 2
                        continue
                    }
                }
            }
            builder.append(char)
            index++
        }
        return builder.toString()
    }

    override fun isEnabled(level: SentryLevel?): Boolean =
        debug || level == SentryLevel.ERROR || level == SentryLevel.FATAL

    private fun emit(level: SentryLevel, throwable: Throwable?, message: String) {
        if (!isEnabled(level)) return
        when (level) {
            SentryLevel.ERROR, SentryLevel.FATAL -> logger.warn("[sentry] $message", throwable)
            else -> logger.info("[sentry] $message")
        }
    }
}
