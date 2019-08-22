FROM adoptopenjdk/openjdk8:alpine-jre
RUN mkdir /app
USER 1000
WORKDIR /app

COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://ci.velocitypowered.com/job/velocity/164/artifact/proxy/build/libs/velocity-proxy-1.0.2-all.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
