plugins {
    id("java")
    id("jacoco")
}

group = "org.server.virtual.threads"
version = "1.0-SNAPSHOT"

jacoco {
    toolVersion = "0.8.14"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
}

// ---- Setting up Java 25 and preview features ----
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(false)
        csv.required.set(false)
        html.required.set(true)
    }

    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.map { dir ->
            fileTree(dir).apply {
                exclude("org/server/virtual/threads/app/**")
                exclude("org/server/virtual/threads/server/config/**")
                exclude("org/server/virtual/threads/core/constants/**")
                exclude("org/server/virtual/threads/core/model/**")
            }
        }
    )
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}