plugins {
    // Используем стандартный плагин Kotlin JVM и сериализации
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"

    // Стандартный плагин Gradle для запуска приложения (заменяет io.ktor.plugin)
    application
}

group = "com.example"
version = "0.0.1"
kotlin {
    jvmToolchain(21) // или 17, в зависимости от установленной JDK
}
application {
    // Главный класс Ktor для запуска через Netty
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
val exposedVersion = "0.50.1"

dependencies {
    // 🌐 Ktor Server (Движок Netty + Ядро)
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml:${ktorVersion}")
    // 🔄 Сериализация (JSON)
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    implementation("io.ktor:ktor-server-status-pages:${ktorVersion}")

    // 🔒 Безопасность: JWT + BCrypt
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("org.mindrot:jbcrypt:0.4")
    // 🗄️ Работа с БД (Exposed ORM + PostgreSQL)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.3")

    // 📝 Логирование
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // 🧪 Тесты (БД в памяти H2, чтобы не требовался запущенный PostgreSQL)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")
    testImplementation("com.h2database:h2:2.2.224")
}
