dependencies {
    compileOnly(libs.simplecloud.api)
    implementation(rootProject.libs.bundles.cloud.core)
    compileOnly(rootProject.libs.bundles.adventure)

    api("org.spongepowered:configurate-yaml:4.0.0")
    api("org.spongepowered:configurate-extra-kotlin:4.1.2")

    compileOnly("ch.qos.logback:logback-classic:1.5.25")

    compileOnly("com.google.guava:guava:33.5.0-jre")
}