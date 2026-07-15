FROM amazoncorretto:21-alpine

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Shanghai"

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app app \
    && mkdir -p /app/logs /app/uploads \
    && chown -R app:app /app

COPY hive-backend.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
