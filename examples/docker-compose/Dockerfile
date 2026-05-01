# syntax=docker/dockerfile:1.7
# Build stage: compile the sample app from the project source tree.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependency resolution by copying poms first.
COPY pom.xml ./
COPY spring-aiact-core/pom.xml spring-aiact-core/
COPY spring-aiact-codegen/pom.xml spring-aiact-codegen/
COPY spring-aiact-spring-boot-starter/pom.xml spring-aiact-spring-boot-starter/
COPY spring-aiact-maven-plugin/pom.xml spring-aiact-maven-plugin/
COPY spring-aiact-sample/pom.xml spring-aiact-sample/
RUN mvn -B -ntp -q -Dmaven.test.skip=true dependency:go-offline || true

# Then copy sources and build.
COPY . .
RUN mvn -B -ntp -q -DskipTests -pl spring-aiact-sample -am package

# Runtime stage: minimal JRE image.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /opt/aiact
COPY --from=build /workspace/spring-aiact-sample/target/spring-aiact-sample-*.jar app.jar
RUN useradd -r aiact && mkdir -p /var/log/aiact && chown -R aiact:aiact /var/log/aiact
USER aiact
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/opt/aiact/app.jar"]
