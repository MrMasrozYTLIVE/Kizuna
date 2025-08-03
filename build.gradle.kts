plugins {
    java
    `maven-publish`
}

group = "dev.mitask"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("org.projectlombok:lombok:${project.properties["lombok_version"]}")
    annotationProcessor("org.projectlombok:lombok:${project.properties["lombok_version"]}")

    implementation("gg.jte:jte:${project.properties["jte_version"]}")

    implementation("com.google.code.gson:gson:${project.properties["gson_version"]}")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "kizuna"
            from(components["java"])
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://maven.mitask.dev/releases"
            val snapshotsRepoUrl = "https://maven.mitask.dev/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            val credentials = providers.credentials(PasswordCredentials::class, "mitaskMaven").get()
            credentials {
                username = credentials.username
                password = credentials.password
            }
        }
    }
}


tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}