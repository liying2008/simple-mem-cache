import org.jreleaser.model.Active

plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.jreleaser)
    `maven-publish`
}

group = "cc.duduhuo"
version = "1.2.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.kotlinStdlibJdk8)
    testImplementation(kotlin("test"))
}

tasks.getByName<Jar>("jar") {
    manifest {
        attributes(mapOf("Implementation-Title" to "Simple Mem Cache"))
        attributes(mapOf("Implementation-Version" to archiveVersion))
        attributes(mapOf("Implementation-Vendor" to "Li Ying"))
        attributes(mapOf("Built-By" to System.getProperty("user.name")))
        attributes(mapOf("Built-JDK" to System.getProperty("java.version")))
        attributes(mapOf("Built-Gradle" to gradle.gradleVersion))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "simple-mem-cache"

            from(components["java"])

            pom {
                name = "Simple Mem Cache"
                description =
                    "A lightweight, high-performance, and dependency-free in-memory cache solution with TTL and concurrent access support, ideal for storing hot data in web applications through a minimalistic, ready-to-use API."
                url = "https://github.com/liying2008/simple-mem-cache"
                inceptionYear = "2025"
                packaging = "jar"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/license/MIT"
                    }
                }
                developers {
                    developer {
                        name = "Li Ying"
                        email = "liruoer2008@yeah.net"
                        url = "https://github.com/liying2008"
                    }
                }
                scm {
                    url = "https://github.com/liying2008/simple-mem-cache"
                    connection = "scm:git:https://github.com/liying2008/simple-mem-cache.git"
                    developerConnection = "scm:git:https://github.com/liying2008/simple-mem-cache.git"
                }
            }
        }
    }
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    gitRootSearch = true
    signing {
        active = Active.ALWAYS
        armored = true
    }
    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.path)
                }
            }
        }
    }
}
