import java.net.URI

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
        val baseName = project.property("archives_base_name") as String

        this.archivesName.set(if (baseName.equals("endec")) baseName else "endec.${baseName}")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.1.0")

        implementation("it.unimi.dsi:fastutil:8.5.12")
        implementation("com.google.guava:guava:32.1.2-jre")

        testImplementation("org.junit.jupiter:junit-jupiter:5.11.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        testImplementation(rootProject.project("gson"))
        testImplementation(rootProject.project("jankson"))
        testImplementation(rootProject.project("netty"))

        testImplementation("org.jetbrains:annotations:24.1.0")
        testImplementation("io.netty:netty-buffer:4.1.97.Final")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        maxHeapSize = "1G"

        testLogging {
            events("passed")
        }
    }

    tasks.publish { dependsOn(tasks.check) }
    tasks.publishToMavenLocal { dependsOn(tasks.check) }

    val targetJavaVersion = 17
    tasks.withType<JavaCompile>().configureEach {
        this.options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
            this.options.release = targetJavaVersion
        }
    }

    java {
        withSourcesJar()

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        if (project.name == "endec_test") return@publishing;

        publications {
            create<MavenPublication>("mavenCommon") {
                groupId = project.property("maven_group") as String;
                from(components["java"])
            }
        }

        repositories {
            val env = System.getenv()
            maven {
                url = URI.create(env["MAVEN_URL"] ?: "")
                credentials {
                    username = env["MAVEN_USER"]
                    password = env["MAVEN_PASSWORD"]
                }
            }
        }
    }
}