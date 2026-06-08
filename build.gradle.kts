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
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.mockito:mockito-core:5.16.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.46")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.4.0")
    compileOnly("org.springframework:spring-webmvc:6.2.3")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

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

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
    }
    include("com/vts/Vts.java")
    include("com/vts/VtsServer.java")
    include("com/vts/server/config/ServerConfig.java")
}

tasks.register("javadocJar", Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
    }

    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.map { dir ->
            fileTree(dir).apply {
                exclude("org/server/virtual/threads/app/**")
                exclude("org/server/virtual/threads/server/config/**")
                exclude("org/server/virtual/threads/core/constants/**")
                exclude("org/server/virtual/threads/core/model/**")
                exclude("org/server/virtual/threads/core/handler/**")
                exclude("org/server/virtual/threads/core/router/Router.java")
            }
        }
    )
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}