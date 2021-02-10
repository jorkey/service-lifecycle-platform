#!/bin/bash -e

function exitUsage() {
  >&2 echo "Use: $0 <cloudProvider> <distributionName> <distributionTitle> <mongoDbName>"
  exit 1
}

cloudProvider=$1
distributionName=$2
distributionTitle=$3
mongoDbName=$4

jq '.name="${distributionName}" | .title="${distributionTitle}" | .instanceId="test_instance" | .mongoDb.name="${mongoDbName}"' distribution_pattern.json > distribution.json

./distribution.sh