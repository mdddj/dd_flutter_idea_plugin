val dartVersion: String by project
val flutterVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val ideaVersion: String by project
val ideaType: String by project
val pluginVersion: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20-RC"
    idea
}
group = "shop.itbug"
version = pluginVersion

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}




intellij {
    version.set(ideaVersion)
    type.set(ideaType)
    plugins.set(
        listOf(
            "yaml",
            "Dart:$dartVersion",
            "io.flutter:$flutterVersion",
            "org.intellij.plugins.markdown",
            "terminal",
        )
    )
}
kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }


}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:latest.release")
    implementation("com.squareup.retrofit2:converter-gson:latest.release")
    implementation("cn.hutool:hutool-all:latest.release")
    implementation("org.smartboot.socket:aio-core:latest.release")
    implementation("com.alibaba.fastjson2:fastjson2:latest.release")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:latest.release")
    implementation("com.google.code.gson:gson:latest.release")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
}


var javaVersion = "17"
tasks {

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        compilerOptions {

        }
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
        changeNotes.set(
            """
                <div>
                     <h1>3.8.4.Iguana</h1>
                     <ul>
                        <li>Add the ability to set default values (freezed)</li>
                     </ul>
                </div>
                <div>
                     <h1>3.8.3</h1>
                     <ul>
                        <li>Removed the context menu: "Covert to By Flutterx", which didn't help much</li>
                        <li>Removed the "Favorite Plugins" tool window and its related functions.</li>
                        <li>Fix the problem that "SP" can not be displayed</li>
                     </ul>
                </div>
                <div>
                    <h1>3.8.0</h1>
                    <ul>
                        <li>Add compact mode layout to the Dio url list</li>
                        <li>Add Hive tool, document:<a href='https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/hive.md' alt='hive doc'>document</a> (Bete))</li>
                        <li>Optimize partial connection logic</li>
                     </ul>
                </div>
                <div>
                    <h1>3.7.0</h1>
                    <ul>
                        <li>Dependency version detection using <pre>ExternalAnnotator</pre> override</li>
                        <li>Change the plugin name to: FlutterX</li>
                        <li>Removed Hive tool window, functionality is currently under development</li>
                        <li>Subsequent version update logs are only written in English</li>
                        <li>Add the 'Ignore Dependency Version Detection' feature</li>
                        <img src='https://github.com/mdddj/dd_flutter_idea_plugin/blob/3.7.0/images/do_not_check.png?raw=true' />
                        <li>
                            New parameter inline display
                            <img src='https://github.com/mdddj/dd_flutter_idea_plugin/blob/3.7.0/images/inlay_param_new.png?raw=true' />
                        </li>
                        
                     </ul>
                </div>
                
                <div>
                    <h1>3.6.1</h1>
                    <ul>
                        <li>Bug 修复</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>Bug fix</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>bug修復</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>けっかん修复</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>결함修复</li>
                     </ul>
                </div>
                
                <div>
                    <h1>3.6.0</h1>
                    <ul>
                        <li>Bug 修复</li>
                        <li>添加shared_preferences查看工具</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>Bug fix</li>
                        <li>Add `shared_Preferences` viewing tool</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>bug修復</li>
                        <li>添加“shared_Preferences”查看工具</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>けっかん修复</li>
                        <li>shared _ Preferences表示ツールの追加</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>결함修复</li>
                        <li>shared_Preferences 보기 도구 추가</li>
                     </ul>
                </div>
                <div>
                    <h1>3.5.1.as (2023-07-27)</h1>
                    <ul>
                        <li>Add Freezed Shortcut Menu</li>
                        <li>Added multiple languages such as Korean, Japanese, and Traditional Chinese</li>
                        <li>Optimization of other details</li>
                     </ul>
                     <ul>
                        <li>添加freezed快捷操作菜单</li>
                        <li>新增韩语,日语,繁体等多国语言</li>
                        <li>其他若干细节优化</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>freezedショートカットメニューの追加</li>
                        <li>韓国語、日本語、繁体字など多言語を追加</li>
                        <li>その他の詳細の最適化</li>
                     </ul>
                     <hr/>
                     <ul>
                        <li>freezed 바로 가기 메뉴 추가</li>
                        <li>한국어, 일본어, 번체 등 다국어 추가</li>
                        <li>기타 몇 가지 세부 최적화</li>
                     </ul>
                </div>
                <div>
                    <h1>3.4.1.221</h1>
                     <ul>
                        <li>Fix bug that cannot be enabled in version 3.4.0.221</li>
                     </ul>
                </div>
                <div>
                    <h1>3.4.0.221</h1>
                     <ul>
                        <li>Adaptation 221</li>
                        <li>New: Flutter version detection</li>
                        <li>New: Community Link Entry</li>
                     </ul>
                </div>
                <div>
                    <h1>3.3.3.as</h1>
                     <ul>
                        <li>Optimize the Dio request window tool</li>
                        <li>freezed class model add <pre>ff</pre> command，Quickly generate the from Json function </li>
                        <img src='https://github.com/mdddj/dd_flutter_idea_plugin/blob/3.3.2.as/images/freezed_ff.png?raw=true' />
                     </ul>
                </div>
                <div>
                    <h1>3.3.2.as</h1>
                     <ul>
                        <li>Optimize the Dio request window tool</li>
                        <li>OpenAI function removal</li>
                     </ul>
                </div>
                
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


