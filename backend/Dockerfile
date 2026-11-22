FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/target/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=demo
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Dfile.encoding=UTF-8"

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
