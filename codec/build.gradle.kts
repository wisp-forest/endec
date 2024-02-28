import java.net.URI

repositories {
    mavenCentral()
    maven {
        url = URI.create("https://libraries.minecraft.net")
    }
}

dependencies {
    compileOnly("com.mojang:datafixerupper:6.0.8")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly(rootProject)
}

