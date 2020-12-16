import org.gradle.internal.jvm.Jvm
import org.gradle.util.GradleVersion
import java.time.Instant
import java.util.jar.Attributes.Name

plugins {
    `java-library`
    `maven-publish`
    jacoco
    signing
    id(PluginLibs.sonarQube) version PluginLibs.Version.sonarQube
    id(PluginLibs.nexusStaging) version PluginLibs.Version.nexusStaging
    id(PluginLibs.jooq) version PluginLibs.Version.jooq apply false
}
val jacocoHtml: String? by project
val semanticVersion: String by project
val buildHash: String by project

allprojects {
    group = "io.github.zero88.msa.sql"

    repositories {
        mavenLocal()
        maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
        mavenCentral()
        jcenter()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "eclipse")
    apply(plugin = "idea")
    apply(plugin = "jacoco")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")
    project.version = "$version$semanticVersion"
    project.group = ProjectUtils.computeGroup(project)
    project.ext.set("title", findProperty("title") ?: project.name)
    project.ext.set("baseName", ProjectUtils.computeBaseName(project))
    project.ext.set("description", findProperty("description") ?: "Microservices Architecture SQL: ${project.name}")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        withJavadocJar()
        withSourcesJar()
    }

    dependencies {
        compileOnly(UtilLibs.lombok)
        annotationProcessor(UtilLibs.lombok)

        testImplementation(TestLibs.junit5Api)
        testImplementation(TestLibs.junit5Engine)
        testImplementation(TestLibs.jsonAssert)
        testCompileOnly(UtilLibs.lombok)
        testAnnotationProcessor(UtilLibs.lombok)
    }

    tasks {
        jar {
            doFirst {
                println("- Project Name:     ${project.ext.get("baseName")}")
                println("- Project Title:    ${project.ext.get("title")}")
                println("- Project Group:    ${project.group}")
                println("- Project Artifact: ${project.name}")
                println("- Project Version:  ${project.version}")
            }

            archiveBaseName.set(project.ext.get("baseName") as String)
            manifest {
                attributes(
                    mapOf(Name.MANIFEST_VERSION.toString() to "1.0",
                          Name.IMPLEMENTATION_TITLE.toString() to archiveBaseName,
                          Name.IMPLEMENTATION_VERSION.toString() to project.version,
                          "Created-By" to GradleVersion.current(),
                          "Build-Jdk" to Jvm.current(),
                          "Build-By" to project.property("buildBy"),
                          "Build-Hash" to project.property("buildHash"),
                          "Build-Date" to Instant.now())
                )
            }
        }
        javadoc {
            title = "${project.ext.get("title")} ${project.version} API"
            options {
                this as StandardJavadocDocletOptions
                tags = mutableListOf("apiNote:a:API Note:", "implSpec:a:Implementation Requirements:",
                                     "implNote:a:Implementation Note:")
                if (JavaVersion.current().isJava8Compatible) {
                    addBooleanOption("Xdoclint:none", true)
                }
            }
        }
        test {
            useJUnitPlatform()
        }

        withType<Sign>().configureEach {
            onlyIf { project.hasProperty("release") }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group as String?
                artifactId = project.name
                version = project.version as String?
                from(components["java"])

                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set(project.name)
                    description.set(project.ext.get("description") as String)
                    url.set("https://github.com/zero88/msa-sql")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://github.com/zero88/msa-sql/blob/master/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("zero88")
                            email.set("sontt246@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://git@github.com:zero88/msa-sql.git")
                        developerConnection.set("scm:git:ssh://git@github.com:zero88/msa-sql.git")
                        url.set("https://github.com/zero88/msa-sql")
                    }
                }
            }
        }
        repositories {
            maven {
                val path = if (project.hasProperty("github")) {
                    "${project.property("github.nexus.url")}/${project.property("nexus.username")}/${rootProject.name}"
                } else {
                    val releasesRepoUrl = project.property("ossrh.release.url")
                    val snapshotsRepoUrl = project.property("ossrh.snapshot.url")
                    if (project.hasProperty("release")) releasesRepoUrl else snapshotsRepoUrl
                }
                url = path?.let { uri(it) }!!
                credentials {
                    username = project.property("nexus.username") as String?
                    password = project.property("nexus.password") as String?
                }
            }
        }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications["maven"])
    }
}

task<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    dependsOn(subprojects.map { it.tasks.withType<JacocoReport>() })
    additionalSourceDirs.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.map { it.sourceSets.main.get().output })
    executionData.setFrom(project.fileTree(".") {
        include("**/build/jacoco/test.exec")
    })
    reports {
        csv.isEnabled = false
        xml.isEnabled = true
        xml.destination = file("${buildDir}/reports/jacoco/coverage.xml")
        html.isEnabled = (jacocoHtml ?: "true").toBoolean()
        html.destination = file("${buildDir}/reports/jacoco/html")
    }
}

project.tasks["sonarqube"].group = "analysis"
project.tasks["sonarqube"].dependsOn("build", "jacocoRootReport")
sonarqube {
    properties {
        property("jacocoHtml", "false")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/coverage.xml")
    }
}

task<Sign>("sign") {
    dependsOn(subprojects.map { it.tasks.withType<Sign>() })
}

nexusStaging {
    packageGroup = "io.github.zero88"
    username = project.property("nexus.username") as String?
    password = project.property("nexus.password") as String?
}

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
}
