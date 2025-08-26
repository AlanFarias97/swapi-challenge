# Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests=true clean package

# Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseContainerSupport -XX:+UseSerialGC"
ENV PORT=8080
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
