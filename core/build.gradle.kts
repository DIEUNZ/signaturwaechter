import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))

    // An example of how to add an external dependency that is used by the addon.
    // addonMavenDependency("org.jeasy:easy-random:5.0.0")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Der LabyMod-Annotationsprozessor ist für die reinen Logik-Tests nicht nötig
tasks.named<JavaCompile>("compileTestJava") {
    options.annotationProcessorPath = files()
    options.compilerArgs.add("-proc:none")
}

// Webhook-URL für "Fehler melden": nur aus Gradle-Property bzw. Umgebungsvariable, nie im Quelltext.
// Ohne Wert bleibt die Konstante leer und das Feature ist in diesem Build deaktiviert.
val webhookUrl = providers.gradleProperty("ahtWebhookUrl")
    .orElse(providers.environmentVariable("AHT_WEBHOOK_URL"))
    .orElse("")
val generatedDir = layout.buildDirectory.dir("generated/buildconfig")

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val url = webhookUrl
    val out = generatedDir
    inputs.property("webhookUrl", url)
    outputs.dir(out)
    doLast {
        val escaped = url.get().trim().replace("\\", "\\\\").replace("\"", "\\\"")
        val file = out.get().file("de/bjarne/sigguard/core/BuildConfig.java").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package de.bjarne.sigguard.core;\n\n" +
                "/** Vom Build erzeugt, nicht committen. */\n" +
                "public final class BuildConfig {\n\n" +
                "  public static final String WEBHOOK_URL = \"$escaped\";\n\n" +
                "  private BuildConfig() {\n  }\n}\n"
        )
    }
}

sourceSets.named("main") {
    java.srcDir(generatedDir)
}

tasks.named("compileJava") {
    dependsOn(generateBuildConfig)
}
