plugins {
    id("java")
}

allprojects {
    apply(plugin = "java")

    this.group = rootProject.property("maven_group") as String
    this.version = project.property("version") as String

    base {
        this.archivesName.set(project.property("archives_base_name") as String)
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")

        implementation("it.unimi.dsi:fastutil:8.5.13")
        implementation("com.google.guava:guava:33.0.0-jre")
    }
}