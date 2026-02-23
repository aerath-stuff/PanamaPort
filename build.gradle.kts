import groovy.lang.Closure

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.maven.publish) apply false
}

subprojects {
    afterEvaluate {
        if (plugins.hasPlugin("com.android.library")) {
            configure<com.android.build.api.dsl.LibraryExtension> {
                compileSdk {
                    version = release(36)
                }

                defaultConfig {
                    minSdk = 26
                }
            }
            configure<JavaPluginExtension> {
                toolchain.languageVersion = JavaLanguageVersion.of(21)
            }
        }

        // disable signing for local maven
        // source: https://github.com/vanniktech/gradle-maven-publish-plugin/issues/1113#issuecomment-3380831894
        plugins.apply("signing")
        gradle.taskGraph.whenReady(object : Closure<Unit>(this) {
            fun doCall(graph: org.gradle.api.execution.TaskExecutionGraph) {
                val isMavenLocal = graph.allTasks.any { it.name == "publishToMavenLocal" || it.path.endsWith("publishToMavenLocal") }
                extensions.getByType(SigningExtension::class.java).isRequired = !isMavenLocal
                logger.lifecycle(">> [${project.name}] - isMavenLocal = $isMavenLocal")
            }
        })
    }
}

