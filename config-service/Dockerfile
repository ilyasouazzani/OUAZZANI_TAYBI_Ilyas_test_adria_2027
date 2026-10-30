FROM eclipse-temurin:17-jre
WORKDIR /app
COPY config-service/target/config-service-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","app.jar"]
