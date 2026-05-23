# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the fat jar from build stage
COPY --from=build /app/target/order-service-1.0-SNAPSHOT.jar app.jar

# Map properties to environment variables if needed
ENTRYPOINT ["sh", "-c", "java -Dgateway.protocol=${GATEWAY_PROTOCOL:-GRPC} -Dgateway.port=${GATEWAY_PORT:-9000} -Dgateway.host=${GATEWAY_HOST:-gateway} -Dservice.host=${SERVICE_HOST:-order-service} -jar app.jar"]
