FROM adoptopenjdk/openjdk16:alpine-jre
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://ci.velocitypowered.com/job/velocity-2.0.0/lastSuccessfulBuild/artifact/proxy/build/libs/velocity-proxy-2.0.0-SNAPSHOT-all.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
