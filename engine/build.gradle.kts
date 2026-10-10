plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(21) }
dependencies { testImplementation("junit:junit:4.13.2") }
tasks.test { useJUnit() }
tasks.register<JavaExec>("evaluateStage7") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.phishshield.sentinel.engine.Stage7Evaluation")
    val label = providers.gradleProperty("evaluationLabel").orElse("final")
    args(rootProject.layout.projectDirectory.file("docs/stage-7-${label.get()}-evaluation.md").asFile.absolutePath, label.get(), providers.gradleProperty("evaluationDataset").orElse("stage7-holdout.tsv").get())
}
tasks.register<JavaExec>("evaluatePrototype") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.phishshield.sentinel.engine.PrototypeEvaluation")
    args(layout.buildDirectory.file("reports/evaluation/prototype-v1.md").get().asFile.absolutePath)
}
