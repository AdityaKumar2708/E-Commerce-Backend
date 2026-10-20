
FROM gradle:8.14-jdk21-alpine AS build

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
