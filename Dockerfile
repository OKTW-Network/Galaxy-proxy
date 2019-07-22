FROM adoptopenjdk/openjdk8-openj9:alpine as builder
WORKDIR /app
COPY . .
RUN ./gradlew --no-daemon build

FROM adoptopenjdk/openjdk8-openj9:alpine-jre
COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://ci.velocitypowered.com/job/velocity/164/artifact/proxy/build/libs/velocity-proxy-1.0.2-all.jar /app/velocity.jar

COPY --chown=1000 --from=builder /app/build/libs /app/plugins

# Run Server
USER 1000
WORKDIR /app
EXPOSE 25565
CMD ["./start.sh"]
