#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:303f9c60d5d75c52585c9e95efbc46d43ae8683efe7dee8763a16d6506681ee1 --link https://fill-data.papermc.io/v1/objects/303f9c60d5d75c52585c9e95efbc46d43ae8683efe7dee8763a16d6506681ee1/velocity-3.4.0-SNAPSHOT-528.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
