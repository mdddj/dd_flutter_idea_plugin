plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("org.jetbrains.intellij") version "1.13.0"
}
group = "shop.itbug"
version = "3.0.0.Giraffe"
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2022.3.2")
    type.set("IC")
    plugins.set(
        listOf(
            "yaml",
            "Dart:223.8617.8",
            "io.flutter:72.1.4",
            "org.intellij.plugins.markdown:223.8617.3",
            "terminal", "java"
        )
    )
    downloadSources.set(true)
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.4")
    implementation("cn.hutool:hutool-all:5.8.12")
    implementation("org.smartboot.socket:aio-core:1.6.2")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.24")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.24")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.hildan.krossbow:krossbow-stomp-core:5.0.0")
    implementation("org.hildan.krossbow:krossbow-websocket-okhttp:5.0.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("com.kitfox.svg:svg-salamander:1.0")
}


var javaVersion = "17"
tasks {
    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set("223.8213.*")
        untilBuild.set("223.8617.*")
        changeNotes.set(
            """
                <div>
                    <h1>3.0.0</h1>
                </div>
                <div>
                    <ul>
                        <li>Major change: change diox to dio</li>
                        <li>Major changes: part of the operation of the dio window is transferred to the right-click menu</li>
                        <li>New function: more complete asset generation function</li>
                    </ul>
                </div>
                
                <div>
                    <h1>2.4.0</h1>
                </div>
                <div>
                    <ul>
                        <li>New asset file generation function, welcome to experience</li>
                    </ul>
                </div>
                
                
                <div>
                    <h1>2.3.2</h1>
                </div>
                <div>
                    <ul>
                        <li>bug fixed</li>
                    </ul>
                </div>
                
                 <div>
                    <h1>2.3.1</h1>
                </div>
                <div>
                    <ul>
                        <li>Editor add tool menu</li>
                        <li>Add a new model object to freezed</li>
                        <li>Detail optimization</li>
                    </ul>
                </div>
            
            <div>
            
             <div>
                    <h1>2.3.0</h1>
                </div>
                <div>
                    <ul>
                        <li>Optimize the jam problem of ymal file</li>
                        <li>Translate Chinese to English</li>
                         <li>Optimize multiple functional details</li>
                    </ul>
                </div>
            
            <div>
            
            <div>
                    <h1>2.2.3</h1>
                </div>
                <div>
                    <ul>
                        <li>Add international language</li>
                        <li>Optimize the relevant functions of the diox listening window</li>
                        <li>Remove the 2023.1 version adaptation and update it on the new branch</li>
                        <li>Update the json to freezed model tool, and set the default value to true</li>
                    </ul>
                </div>
            
            <div>
                <h1>2.2.2</h1>
            </div>
            <ul>
                <li>Adapt to Android studio</li>
                <li>Some details are optimized</>
            </ul>
            <div>
                <h1>2.1.5</h1>
            </div>
            <ul>
                <li>Add the function of automatically generating asset file objects</li>
            </ul>
            
            <div>
                <h1>2.x</h1>
            </div>
            <ul>
                <li>Fix the problem that some dio interfaces cannot listen</li>
                <li>Optimize the asset file pop-up mechanism</li>
            <ul>
        """.trimIndent()
        )
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
        autoReloadPlugins.set(true)
    }


    buildSearchableOptions {
        enabled = false
    }

    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }
}