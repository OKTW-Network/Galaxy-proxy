#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:af292ea5c9c330fc3b367e48c7d1d448b1b3216f597798bfbef662d002b1343b --link https://fill-data.papermc.io/v1/objects/af292ea5c9c330fc3b367e48c7d1d448b1b3216f597798bfbef662d002b1343b/velocity-3.4.0-SNAPSHOT-540.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
