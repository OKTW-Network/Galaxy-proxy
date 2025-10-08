#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:0120f7cf8d8001e32dc621a41e5162d0dd0a410b883227d8a2a83ae2a3aac64a --link https://fill-data.papermc.io/v1/objects/0120f7cf8d8001e32dc621a41e5162d0dd0a410b883227d8a2a83ae2a3aac64a/velocity-3.4.0-SNAPSHOT-541.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
