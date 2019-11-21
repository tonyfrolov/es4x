#!/usr/bin/env bash
set -e

REGISTRY="https://registry.npmjs.org"

if [ "$1" = "local" ]; then
  REGISTRY="http://localhost:4873"
fi

# build
if [ "$1" = "local" ]; then
  # a local build will populate the npm cache instead of pushing to a registry
  mvn -fae -Pio.vertx,io.reactiverse -Dnpm-registry="$REGISTRY" clean generate-sources exec:exec@typedoc exec:exec@npm-cache
else
  echo "login as vertx"
  npm adduser --registry "$REGISTRY"
  mvn -fae -Pio.vertx -Dnpm-registry="$REGISTRY" exec:exec@npm-publish

  echo "login as reactiverse"
  npm adduser --registry "$REGISTRY"
  mvn -fae -Pio.reactiverse -Dnpm-registry="$REGISTRY" exec:exec@npm-publish
fi
