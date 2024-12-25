#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:0ed9a02dcb102b3a665ce9615691df32e919a0e7e986d86e932d52adf1bd3119 --link https://api.papermc.io/v2/projects/velocity/versions/3.4.0-SNAPSHOT/builds/458/downloads/velocity-3.4.0-SNAPSHOT-458.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
