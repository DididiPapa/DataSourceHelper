plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.crudman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.crudman.datasourcehelper")
    mainClass.set("com.crudman.datasourcehelper.Launcher")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.2.1")

    // 数据库 JDBC 驱动
    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:6.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "DataSourceHelper"
    }
}

// ========== 打包成 exe（Windows） ==========
val jpackageDir = layout.buildDirectory.dir("jpackage")

tasks.register<Exec>("createExe") {
    group = "distribution"
    description = "打包成 .exe 安装程序"
    dependsOn("jar")

    // 准备 jars 目录
    val jarsDir = layout.buildDirectory.dir("jars")

    // 删除旧打包结果 + 复制所有 jar + 准备图标
    doFirst {
        // 删除旧输出
        val target = File(jpackageDir.get().asFile, "DataSourceHelper")
        if (target.exists()) target.deleteRecursively()
        // 复制 jars
        val dest = jarsDir.get().asFile
        dest.mkdirs()
        tasks.jar.get().archiveFile.get().asFile.copyTo(
            File(dest, tasks.jar.get().archiveFileName.get()), true)
        configurations.runtimeClasspath.get().forEach { f ->
            f.copyTo(File(dest, f.name), true)
        }
        // 复制 ico 到 jars 目录（app-image 模式图标放在 input 目录中）
        val iconSrc = File("${projectDir}/src/main/resources/com/crudman/datasourcehelper/favicon.ico")
        if (iconSrc.exists()) {
            iconSrc.copyTo(File(dest, "DataSourceHelper.ico"), true)
        }
    }

    val jdkHome = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }.get().metadata.installationPath.asFile.absolutePath

    workingDir = jpackageDir.get().asFile

    commandLine(
        "$jdkHome/bin/jpackage",
        "--type", "app-image",
        "--name", "DataSourceHelper",
        "--app-version", "1.0",
        "--vendor", "crudman",
        "--description", "数据库代码生成器",
        "--input", jarsDir.get().asFile.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "com.crudman.datasourcehelper.Launcher",
        "--dest", jpackageDir.get().asFile.absolutePath,
        "--icon", jarsDir.get().asFile.absolutePath + "/DataSourceHelper.ico"
    )
}
