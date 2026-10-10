plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(21) }
dependencies { testImplementation("junit:junit:4.13.2") }
tasks.test { useJUnit() }
tasks.register<JavaExec>("evaluatePrototype") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.phishshield.sentinel.engine.PrototypeEvaluation")
    args(layout.buildDirectory.file("reports/evaluation/prototype-v1.md").get().asFile.absolutePath)
}
