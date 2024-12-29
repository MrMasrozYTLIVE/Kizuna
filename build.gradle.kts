plugins {
    id("java")
}

group = "net.mitask"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:${project.properties["lombok_version"]}")
    annotationProcessor("org.projectlombok:lombok:${project.properties["lombok_version"]}")

    implementation("gg.jte:jte:${project.properties["jte_version"]}")

    implementation("com.google.code.gson:gson:${project.properties["gson_version"]}")
}