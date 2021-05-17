#!/usr/bin/env ash
# Hack configmap
[[ -e config/velocity.toml ]] && cp -f config/velocity.toml velocity.toml

# Start
exec java -Dvelocity.i-understand-what-im-doing=true -jar velocity.jar
