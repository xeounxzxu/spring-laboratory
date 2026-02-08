# Repository Guidelines

## Project Structure & Module Organization
Source lives in `src/main/kotlin/com/example/demo`, with `DemoApplication.kt` bootstrapping Spring Boot 3.2 and feature controllers such as `HelloController.kt`. Externalized config belongs in `src/main/resources/application.properties`; create profile-specific files there when targeting different Kubernetes environments. Tests mirror the package layout under `src/test/kotlin/com/example/demo`, and the Gradle wrapper plus Kotlin DSL build scripts (`build.gradle.kts`, `settings.gradle.kts`) sit at the root for deterministic builds.

## Build, Test, and Development Commands
- `./gradlew :mvc:jfr-app:bootRun` — start the application locally on Java 21; use when iterating on controllers.
- `./gradlew :mvc:jfr-app:build` — compile Kotlin sources, resolve dependencies, and run the full test suite, producing `mvc/jfr-app/build/libs/*.jar` for container images.
- `./gradlew :mvc:jfr-app:test` — execute JUnit 5/Spring Boot tests only; add `--info` when diagnosing failures.
- `./gradlew :mvc:jfr-app:clean` — drop the `mvc/jfr-app/build/` output to ensure Kubernetes containers pick up fresh artifacts.
- `docker build -t spring-k8s-repo .` — package the Boot JAR with the provided multi-stage Dockerfile (tests run during the build stage).

## Latency & JIT Training Endpoint
`LatencyTrainingController` exposes `GET /latency/probe?iterations=60000&rounds=5&loadClasses=java.time.ZoneId,java.util.UUID` for practicing JVM start-up analysis. The handler runs deterministic CPU-heavy math to trigger JIT compilation, optionally forces class loading via the `loadClasses` query parameter, and returns real-time snapshots from `ClassLoadingMXBean` and `CompilationMXBean`. Pair the endpoint with tools like JFR by supplying `JAVA_OPTS="-XX:StartFlightRecording=duration=120s,disk=true,maxsize=256m,filename=/app/records/startup.jfr"` (ensure `/app/records` exists or is volume-mounted) and collect resulting files via `kubectl cp`.

## Docker & Runtime Images
The Dockerfile uses Eclipse Temurin JDK 21 to run Gradle inside the image and Eclipse Temurin JRE 21 for the runtime layer. Pass JVM options at runtime via `JAVA_OPTS`, for example `docker run -p 8080:8080 -e JAVA_OPTS="-Xms256m" spring-k8s-repo`. CI builders that already produced a JAR can speed things up with `docker build --build-arg BUILDKIT_INLINE_CACHE=1 .` plus cached layers. The `.dockerignore` excludes `.gradle/`, build outputs, and IDE configs to keep contexts small; add any new tooling directories there when needed.

## Coding Style & Naming Conventions
Stick to Kotlin official style: four-space indentation, trailing commas avoided, and braces on the same line as declarations. Classes and configuration objects use UpperCamelCase (`HelloController`), functions and bean methods use lowerCamelCase, and REST paths stay kebab-case (e.g., `/hello`). Keep package names under `com.example.demo` unless introducing a new bounded context, and keep any Spring annotations closely stacked above the declaration for readability.

## Testing Guidelines
JUnit 5 with Spring Boot test slices is already enabled via `spring-boot-starter-test`. Name files `*Tests.kt` to match Gradle’s conventions and isolate fast tests with `@WebMvcTest` when full `@SpringBootTest` is unnecessary. Run `./gradlew :mvc:jfr-app:test` locally before every push; new endpoints should carry controller tests plus any necessary serialization checks. Target at least one regression test per bug fix so coverage around HTTP handlers grows alongside features.

## Commit & Pull Request Guidelines
The canonical history (available when cloning the repo with its `.git` data) uses short, imperative subjects with Conventional Commit prefixes such as `feat:`, `fix:`, or `chore:`; continue that pattern and reference an issue ID when relevant. Each pull request should describe the functional change, list new Gradle tasks or configs touched (e.g., `application.properties` additions), and include screenshots or `curl` traces for API adjustments. Confirm `./gradlew :mvc:jfr-app:build` and `docker build` succeed and mention any Kubernetes manifest updates or environment variables reviewers must set.
