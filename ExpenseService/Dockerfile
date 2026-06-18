FROM eclipse-temurin:21-jdk-jammy
WORKDIR /usr/src/myapp
COPY ExpenseService-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9820
ENTRYPOINT ["java", "-jar", "app.jar"]
