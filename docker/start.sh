# Hack configmap
[ -e config/velocity.toml ] && cp -f config/velocity.toml velocity.toml

# Start
java -jar velocity.jar
