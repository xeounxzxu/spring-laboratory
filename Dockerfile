# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace/app

# Copy source
COPY . .

# Build a bootable JAR (tests run by default; disable with -x test if needed)
RUN ./gradlew --no-daemon bootJar \
 && JAR_PATH=$(find build/libs -name "*.jar" ! -name "*-plain.jar" -print -quit) \
 && test -n "$JAR_PATH" \
 && mv "$JAR_PATH" app.jar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /workspace/app/app.jar ./app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
