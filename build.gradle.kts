allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    // 공통 라이브러리는 실제 JVM 소스셋이 있는 모듈에만 주입한다.
    pluginManager.withPlugin("java") {
        dependencies {
            add("implementation", "io.github.microutils:kotlin-logging:3.0.5")
        }
    }
}
