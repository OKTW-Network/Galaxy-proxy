#syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app && chown 1000:100 /app
USER 1000
WORKDIR /app

COPY --chown=1000 --link docker /app

# Download Velocity
ADD --chown=1000 --checksum=sha256:ef1a852bfae7397e84907837925e7ad21c6312066290edaae401b77f6f423ac3 --link https://fill-data.papermc.io/v1/objects/ef1a852bfae7397e84907837925e7ad21c6312066290edaae401b77f6f423ac3/velocity-3.4.0-SNAPSHOT-558.jar /app/velocity.jar

# Run Server
EXPOSE 25565
CMD ["./start.sh"]
