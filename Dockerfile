FROM maven AS build

WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn package -DskipTests

FROM openjdk:8-jre

WORKDIR /app
COPY --from=build /app/target/libs /app/libs
COPY --from=build /app/target/passthrough-*.jar /app/exposer.jar

CMD java -jar /app/exposer.jar
