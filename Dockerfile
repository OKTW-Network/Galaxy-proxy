#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:845cb2c1891544ad9e432883d7ddf059284d9d178b97be4977e5f805746c9edd --link https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/450/downloads/velocity-3.4.0-SNAPSHOT-450.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
