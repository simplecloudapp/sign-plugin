dependencies {
    compileOnly(libs.simplecloud.api)
    implementation(rootProject.libs.bundles.cloud.core)
    compileOnly(rootProject.libs.bundles.adventure)

    api(rootProject.libs.configurate.yaml)
    api(rootProject.libs.configurate.extra.kotlin)

    compileOnly(rootProject.libs.logback.classic)

    compileOnly(rootProject.libs.guava)
}
