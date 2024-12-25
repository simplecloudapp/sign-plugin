dependencies {
    api(project(":sign-shared"))
    compileOnly(rootProject.libs.paper.api)
    implementation(rootProject.libs.cloud.annotations)
    implementation(rootProject.libs.bundles.cloud.paper)
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
