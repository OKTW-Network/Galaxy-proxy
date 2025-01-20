#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:f9c8f0c8fb8b7f88abf3ee2877599916a7922995062e24b575fe9761f74023cb --link https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/461/downloads/velocity-3.4.0-SNAPSHOT-461.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
