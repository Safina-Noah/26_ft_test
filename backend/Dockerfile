FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' gradlew \
    && sh gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/reponote-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]