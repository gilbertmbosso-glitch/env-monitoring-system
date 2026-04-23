FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
RUN chmod +x mvnw

COPY src src
COPY predict.py .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/predict.py predict.py

ENV SPRING_PROFILES_ACTIVE=railway

EXPOSE 8080

CMD ["sh", "-c", "java -jar app.jar"]
