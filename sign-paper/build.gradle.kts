dependencies {
    compileOnly(rootProject.libs.paper.api)
    implementation(rootProject.libs.bundles.simplecloud.controller)
    implementation(rootProject.libs.bundles.cloud.paper)
    implementation(project(":sign-shared"))
}