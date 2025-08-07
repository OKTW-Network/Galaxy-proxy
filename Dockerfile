#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:61249fa5b9b33bc7e3223581eab6aedad790a295caf0e39da2ff3c8ec9d9117f --link https://fill-data.papermc.io/v1/objects/61249fa5b9b33bc7e3223581eab6aedad790a295caf0e39da2ff3c8ec9d9117f/velocity-3.4.0-SNAPSHOT-523.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
