#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:9b0c812fe6a3335b49caf998e3be431d09f0f66473ac4f4baea0e844910e9021 --link https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/489/downloads/velocity-3.4.0-SNAPSHOT-489.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
