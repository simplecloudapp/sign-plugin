import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val baseVersion = "0.0.2"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotversion else baseVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.jvm)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks {
        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }

        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }

        named("shadowJar", ShadowJar::class) {
            mergeServiceFiles()
            relocate("com.google.protobuf", "app.simplecloud.relocate.google.protobuf")
            relocate("com.google.common", "app.simplecloud.relocate.google.common")
            relocate("io.grpc", "app.simplecloud.relocate.io.grpc")


            relocate("org.incendo", "app.simplecloud.signs.plugin.relocate.incendo")
            relocate("org.spongepowered", "app.simplecloud.signs.plugin.relocate.spongepowered")
            relocate("app.simplecloud.plugin.api", "app.simplecloud.signs.plugin.relocate.plugin.api")
            archiveFileName.set("${project.name}.jar")
        }

        processResources {
            expand(
                "version" to project.version,
                "name" to project.name
            )
        }
    }
}

tasks.processResources {
    expand(
        "version" to project.version,
        "name" to project.name
    )
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}