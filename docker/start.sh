#!/usr/bin/env bash
# Hack configmap
[[ -e config/velocity.toml ]] && cp -f config/velocity.toml velocity.toml

# Start
exec java -jar velocity.jar
