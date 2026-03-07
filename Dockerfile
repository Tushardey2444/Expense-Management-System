FROM eclipse-temurin:21-jdk
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} expense.jar
CMD ["java", "-jar", "expense.jar"]