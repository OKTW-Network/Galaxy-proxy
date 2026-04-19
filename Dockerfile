#syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:25bfbee6155fbce24f709bf18f1bb915817c4151d6d418ca01282742ab1f123a --link https://fill-data.papermc.io/v1/objects/25bfbee6155fbce24f709bf18f1bb915817c4151d6d418ca01282742ab1f123a/velocity-3.5.0-SNAPSHOT-593.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
