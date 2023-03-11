#syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --link https://api.papermc.io/v2/projects/velocity/versions/3.2.0-SNAPSHOT/builds/236/downloads/velocity-3.2.0-SNAPSHOT-236.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
