plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":sign-shared"))
    compileOnly(rootProject.libs.paper.api)
    implementation(rootProject.libs.bundles.cloud.paper)
    implementation(rootProject.libs.bundles.coroutine)
}

tasks {
    shadowJar {
        relocate("io.grpc", "app.simplecloud.relocate.grpc")
        relocate("app.simplecloud.controller", "app.simplecloud.relocate.controller")
        relocate("app.simplecloud.pubsub", "app.simplecloud.relocate.pubsub")
        relocate("app.simplecloud.droplet", "app.simplecloud.relocate.droplet")
        relocate("build.buf.gen", "app.simplecloud.relocate.buf")
        relocate("com.google.protobuf", "app.simplecloud.relocate.protobuf")
    }
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("M2XJERK4")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4"
    )
    loaders.add("paper")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}