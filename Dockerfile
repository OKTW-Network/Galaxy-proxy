#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:bb54dcd34751bf8e345d0a39199eb985d64d07f8626810ed7b0e058f47d595c0 --link https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/453/downloads/velocity-3.4.0-SNAPSHOT-453.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
