FROM ibm-semeru-runtimes:open-16-jre
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://versions.velocitypowered.com/download/3.0.1.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
