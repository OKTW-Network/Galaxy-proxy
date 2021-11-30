FROM ibm-semeru-runtimes:open-16-jre
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 docker /app

# Download Velocity
ADD --chown=1000 https://papermc.io/api/v2/projects/velocity/versions/3.1.0/builds/95/downloads/velocity-3.1.0-95.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
