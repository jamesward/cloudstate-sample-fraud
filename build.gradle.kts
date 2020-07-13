import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

val grpcVersion = "1.30.0"
val protobufVersion = "3.11.4"
val grpcKotlinVersion = "0.1.3"
val coroutinesVersion = "1.3.7"

//val protobufVersion = "3.12.2"

plugins {
    application
    kotlin("jvm") version "1.3.72"
    id("com.google.protobuf") version "0.8.12"
}

repositories {
    mavenLocal()
    google()
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("io.cloudstate:cloudstate-kotlin-support:0.5.1")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("com.google.api.grpc:proto-google-common-protos:1.18.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.apache.lucene:lucene-spatial:8.4.1")
    implementation("com.devskiller:jfairy:0.6.4")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    val main by getting
    main.java.srcDirs("build/generated/source/proto/main/java")
    main.java.srcDirs("build/generated/source/proto/main/grpc")
    main.java.srcDirs("build/generated/source/proto/main/grpckt")

    /*
    main {

        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
        kotlin {
            srcDirs("")
        }
    }
     */
}

application {
    mainClassName = "com.google.cloudstate.sample.fraud.ServerKt"
}

tasks.register<JavaExec>("simulator") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "com.google.cloudstate.sample.fraud.Simulator"
}

task("stage").dependsOn("installDist")
