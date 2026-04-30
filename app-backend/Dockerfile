# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# 先复制 Gradle 配置文件，利用 Docker 缓存层
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/

# 下载依赖（利用缓存，源码不变时不会重新下载）
RUN ./gradlew dependencies --no-daemon -q

# 复制源码
COPY src/ src/

# 构建
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
