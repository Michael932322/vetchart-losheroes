plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.vetsolutions"
version = "2.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.vetsolutions.vetchart.MainKt")
    // Permite leer datos desde la consola al ejecutar con ./gradlew run
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
