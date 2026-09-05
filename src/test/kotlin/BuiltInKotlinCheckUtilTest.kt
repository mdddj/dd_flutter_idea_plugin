import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import shop.itbug.flutterx.util.BuiltInKotlinCheckUtil
import shop.itbug.flutterx.util.BuiltInKotlinStatus

class BuiltInKotlinCheckUtilTest {

    private fun check(gradleText: String, kotlinFileCount: Int = 1) =
        BuiltInKotlinCheckUtil.inspect("test_package", gradleText, kotlinFileCount)

    @Test
    fun testMigratedNoKgp() {
        // 已迁移：无 KGP，使用顶层 kotlin.compilerOptions
        val result = check(
            """
            plugins {
                id("com.android.library")
            }
            kotlin {
                compilerOptions {
                    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
                }
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.MIGRATED, result.status)
    }

    @Test
    fun testMigratedPlainJava() {
        // 无 Kotlin DSL 痕迹
        val result = check(
            """
            plugins {
                id("com.android.library")
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.MIGRATED, result.status)
    }

    @Test
    fun testCompatibleConditionalKgp() {
        // 条件式应用 KGP：agpMajor < 9 才应用
        val result = check(
            """
            val agpMajor = com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt()
            if (agpMajor < 9) {
                apply(plugin = "org.jetbrains.kotlin.android")
            }
            kotlin {
                compilerOptions {
                    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
                }
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.COMPATIBLE, result.status)
        assertTrue(result.reasons.contains("conditional KGP"))
    }

    @Test
    fun testUnmigratedUnconditionalKgpKotlinDsl() {
        val result = check(
            """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.android")
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
        assertTrue(result.reasons.contains("unconditional KGP"))
    }

    @Test
    fun testUnmigratedGroovyLegacyApply() {
        val result = check(
            """
            apply plugin: 'com.android.library'
            apply plugin: 'kotlin-android'
            android {
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_17.toString()
                }
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
        assertTrue(result.reasons.contains("unconditional KGP"))
        assertTrue(result.reasons.contains("kotlinOptions"))
    }

    @Test
    fun testUnmigratedKotlinOptionsWithoutCompilerOptions() {
        // 已移除 KGP 但仍残留 kotlinOptions，AGP 9.0 下会失败
        val result = check(
            """
            plugins {
                id("com.android.library")
            }
            android {
                kotlinOptions {
                    jvmTarget = JavaVersion.VERSION_17.toString()
                }
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
        assertTrue(result.reasons.contains("kotlinOptions"))
    }

    @Test
    fun testUnmigratedKotlinDslApply() {
        // Kotlin DSL 顶层无条件 apply(plugin = "org.jetbrains.kotlin.android")
        val result = check(
            """
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.android")
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
        assertTrue(result.reasons.contains("unconditional KGP"))
    }

    @Test
    fun testUnmigratedGroovyPluginsBlock() {
        val result = check(
            """
            plugins {
                id 'com.android.library'
                id 'org.jetbrains.kotlin.android'
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
    }

    @Test
    fun testBuiltInKotlinFlagDoesNotMakeKgpConditional() {
        val result = check(
            """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.android")
            }
            android.builtInKotlin = true
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
    }

    @Test
    fun testUnmigratedKapt() {
        val result = check(
            """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.kapt")
            }
            """.trimIndent()
        )
        assertEquals(BuiltInKotlinStatus.UNMIGRATED, result.status)
        assertTrue(result.reasons.contains("kapt"))
    }

    @Test
    fun testNoKotlinSource() {
        val result = check("""plugins { id("com.android.library") }""", kotlinFileCount = 0)
        assertEquals(BuiltInKotlinStatus.NO_KOTLIN, result.status)
    }
}
