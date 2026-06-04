FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Install shared module into the local Maven repo first
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
RUN mvn -f shared/pom.xml install -q

# Download service dependencies (cached separately from source)
COPY whereami/pom.xml pom.xml
RUN mvn dependency:go-offline -q

COPY whereami/src src
RUN mvn package -DskipTests -q

FROM registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24
COPY --from=builder --chown=185 /app/target/quarkus-app/ /deployments/
EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]