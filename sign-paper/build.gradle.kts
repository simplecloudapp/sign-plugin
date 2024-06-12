dependencies {
    compileOnly(rootProject.libs.paperApi)
    implementation(rootProject.libs.bundles.simpleCloudController)
    implementation(rootProject.libs.bundles.cloudPaper)
    implementation(project(":sign-shared"))
}