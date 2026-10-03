#!/bin/sh

set -e

docker run --rm \
  -v "${PWD}:/local" \
  openapitools/openapi-generator-cli generate \
  -i https://fill.papermc.io/openapi.yaml \
  -g java \
  --library microprofile \
  -o /local/paper-fill-api/generated-client \
  --api-package=io.github.headlesshq.headlessmc.platform.paper.api \
  --model-package=io.github.headlesshq.headlessmc.platform.paper.api \
  --invoker-package=io.github.headlesshq.headlessmc.platform.paper.api \
  --additional-properties=useJakartaEe=true,interfaceOnly=true,serializationLibrary=jackson,useRecords=true
