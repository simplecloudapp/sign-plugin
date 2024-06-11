dependencies {
    implementation(rootProject.libs.bundles.simpleCloudController)
    implementation(project(":sign-shared"))
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
}