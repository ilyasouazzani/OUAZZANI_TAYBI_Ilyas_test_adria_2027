FROM eclipse-temurin:17-jre
WORKDIR /app
COPY ai-service/target/ai-service-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","app.jar"]
