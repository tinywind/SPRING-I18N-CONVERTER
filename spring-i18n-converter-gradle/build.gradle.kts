import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.2.1"
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation("org.tinywind:spring-i18n-converter:7.0.10")
    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/tinywind/SPRING-I18N-CONVERTER"
    vcsUrl = "https://github.com/tinywind/SPRING-I18N-CONVERTER.git"
    plugins {
        create("springI18nConverter") {
            id = "org.tinywind.spring-i18n-converter"
            implementationClass = "org.tinywind.springi18nconverter.gradle.SpringI18nConverterPlugin"
            displayName = "Spring i18n Converter"
            description = "Converts Spring message properties to JavaScript or Excel and back using XML configuration."
            tags.set(listOf("spring", "i18n", "properties", "javascript", "excel"))
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Spring i18n Converter Gradle Plugin"
            description = "Gradle integration for Spring message properties, JavaScript, and Excel conversion."
            url = "https://github.com/tinywind/SPRING-I18N-CONVERTER"
            licenses {
                license {
                    name = "MIT License"
                    url = "https://opensource.org/license/mit"
                }
            }
            developers {
                developer {
                    id = "tinywind"
                    name = "tinywind"
                    url = "https://github.com/tinywind"
                }
            }
            scm {
                connection = "scm:git:https://github.com/tinywind/SPRING-I18N-CONVERTER.git"
                developerConnection = "scm:git:ssh://git@github.com/tinywind/SPRING-I18N-CONVERTER.git"
                url = "https://github.com/tinywind/SPRING-I18N-CONVERTER"
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
    val testTemp = rootProject.layout.projectDirectory.dir(".tmp/gradle-test-temp")
    val testKit = rootProject.layout.projectDirectory.dir(".tmp/gradle-testkit")
    systemProperty("java.io.tmpdir", testTemp.asFile.absolutePath)
    systemProperty("gradle.testkit.dir", testKit.asFile.absolutePath)
    doFirst {
        testTemp.asFile.mkdirs()
        testKit.asFile.mkdirs()
    }
}
