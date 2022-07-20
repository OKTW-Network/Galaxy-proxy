FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://api.papermc.io/v2/projects/velocity/versions/3.1.2-SNAPSHOT/builds/161/downloads/velocity-3.1.2-SNAPSHOT-161.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
