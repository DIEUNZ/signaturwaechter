plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "de.bjarne"
version = providers.environmentVariable("VERSION").getOrElse("1.0.0")

labyMod {
    defaultPackageName = "de.bjarne.sigguard"

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // When the property is set to true, you can log in with a Minecraft account
                    // devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "sigguard"
        displayName = "AH-Fake-Warner"
        author = "bjarne"
        description = "Schützt im GrieferGames-Auktionshaus vor dem Kauf gefälschter Perks, Prefixe, Suffixe und anderer Admin-Items: Das Addon erkennt Fake-Signaturen, warnt dich und erinnert an ablaufende Auktionen. Arbeitet nur lokal."
        minecraftVersion = "1.8.9,1.12.2,26.3"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}