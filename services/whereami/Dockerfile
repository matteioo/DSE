FROM eclipse-temurin:25-jre
WORKDIR /app

COPY target/quarkus-app/lib/ ./lib/
COPY target/quarkus-app/*.jar ./
COPY target/quarkus-app/app/ ./app/
COPY target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8082
CMD ["java", "-jar", "quarkus-run.jar"]