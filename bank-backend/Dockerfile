# 1. 使用 Java 17 基礎映像檔 (配專案設定)
FROM eclipse-temurin:17-jdk-alpine

# 2. 設定容器內的工作目錄
WORKDIR /app

# 3. 將剛剛打包好的 JAR 檔複製進去，並改名為 app.jar
COPY target/banking-app-0.0.1-SNAPSHOT.jar app.jar

# 4. 容器啟動時執行的指令
ENTRYPOINT ["java", "-jar", "app.jar"]