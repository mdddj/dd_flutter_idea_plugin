plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("org.jetbrains.intellij") version "1.13.3"
}
group = "shop.itbug"
version = "3.3.1.as"
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}


intellij {
    version.set("2022.2.1.18")
    type.set("AI")
    plugins.set(
        listOf(
            "yaml",
            "Dart:222.4560",
            "io.flutter:73.0.2",
            "org.intellij.plugins.markdown",
            "terminal", "java"
        )
    )
}

///
dependencies {
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.5")
    implementation("cn.hutool:hutool-all:5.8.15")
    implementation("org.smartboot.socket:aio-core:1.6.3")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.25")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.25")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation("com.aallam.openai:openai-client:3.2.0") {
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("com.squareup.retrofit2:retrofit:2.9.0"){
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC"){
        exclude(group = "org.slf4j")
    }
}


var javaVersion = "17"
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("222.*")
        changeNotes.set(
            """
                <div>
                    <h1>3.3.1</h1>
                     <ul>
                        <li>Add openai service</li>
                        <li>bug fixed</li>
                     </ul>
                </div>
                
                <div>
                    <h1>3.3.0</h1>
                     <ul>
                        <li>Adaptation 2023.1</li>
                        <li>bug fixed</li>
                     </ul>
                </div>
                
                <div>
                    <h1>3.2.0</h1>
                    <ul>
                        <li>Add a favorite plugin function, which will be saved in the local sqlite database.</br> File default path<pre>~/FlutterCheckVersionXNote.db</pre></li>
                    </ul>
                </div>
                
                <div>
                    <h1>3.1.0</h1>
                </div>
                <div>
                    <ul>
                        <li>Optimize the frozen to model tool</li>
                        <li>I18n adaptation</li>
                    </ul>
                </div>
                
                
                <div>
                    <h1>3.0.4</h1>
                </div>
                <div>
                    <ul>
                        <li>Update dart document display content</li>
                        <li>New feature: add a markdown editor for dart documents</li>
                    </ul>
                </div>
                
                
                
                <div>
                    <h1>3.0.2</h1>
                </div>
                <div>
                    <ul>
                        <li>fix: Fix the problem that the item selector cannot be clicked under multiple windows</li>
                        <li>fix: Fix the problem of clicking the API item to report errors after the project is destroyed</li>
                    </ul>
                </div>
                
                
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
        jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition")
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

configurations.all {
    exclude("org.jetbrains.kotlinx")
}
