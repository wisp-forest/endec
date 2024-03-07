plugins {
    id("java")
    id("maven-publish")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    this.group = rootProject.property("maven_group") as String
    this.version = project.property("version") as String

    base {
        var baseName = project.property("archives_base_name") as String

        this.archivesName.set(if(baseName.equals("endec")) baseName else "endec.${baseName}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")

        implementation("it.unimi.dsi:fastutil:8.5.13")
        implementation("com.google.guava:guava:33.0.0-jre")
    }

    java {
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenCommon") {
                artifactId = project.base.archivesName.get()
                from(components.getByName("java"))
            }
        }

        // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
        repositories {
            // Add repositories to publish to here.
        }
    }
}