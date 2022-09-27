plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.9.0"
}
group = "shop.itbug"
version = "2.0.4"
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}
intellij {
    version.set("2022.2.2")
    type.set("IC")
    plugins.set(
        listOf(
            "yaml",
            "Dart:222.4167.21",
            "org.intellij.plugins.markdown:222.4167.22"
        )
    )

}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("cn.hutool:hutool-all:5.8.6")
    implementation("org.smartboot.socket:aio-core:1.6.0")
    implementation("com.alibaba:fastjson:2.0.13.graal")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
    implementation ("com.fifesoft:rsyntaxtextarea:3.2.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.0")
    implementation("org.slf4j:slf4j-api:2.0.0")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set("212.*")
        untilBuild.set("223.*")
        changeNotes.set("""
            v2.0.4: 更新pubspec.yaml的快捷图标位置
            
            v2.0.2: 新增riverpod代码模板 `conf`和`conl`
             
            v2.0.3: 修复了一些小错误
        """.trimIndent())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
//        jbrVariant.set("dcevm")
//        autoReloadPlugins.set(true)
    }
}
