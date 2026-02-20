FROM eclipse-temurin:25-jre-noble

# curl for healthcheck; python3-pip + edge-tts for Bulgarian TTS audio generation
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl python3-pip && \
    pip3 install --break-system-packages edge-tts && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/bulgarian-vocabulary-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
