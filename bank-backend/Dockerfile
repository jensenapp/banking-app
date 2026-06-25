# Stage 1: Build the Spring Boot application
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
# 下載依賴以加速後續建置
RUN mvn dependency:go-offline
COPY src ./src
# 打包 jar 檔並跳過測試
RUN mvn clean package -DskipTests
# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 從 Stage 1 複製打包好的 jar 檔
COPY --from=build /app/target/*.jar app.jar
# 暴露容器內的 8080 port (Spring Boot 預設)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]