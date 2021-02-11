#!/bin/bash -e
set -e

function exitUsage() {
  >&2 echo "Use: $0 <cloudProvider> <distributionName> <distributionTitle> <mongoDbName>"
  exit 1
}

if [ $# -ne 4 ]; then
  exitUsage
fi

cloudProvider=$1
distributionName=$2
distributionTitle=$3
mongoDbName=$4

if [ "${cloudProvider}" == "Azure" ]; then
  if ! instanceId=`curl --silent -H "Metadata: True" http://169.254.169.254/metadata/instance?api-version=2019-06-01 | jq -rj '.compute.resourceGroupName, ":", .compute.name'`; then
    >&2 echo "Can't get instance Id"
    exit 1
  fi
elif [ "${cloudProvider}" == "None" ]; then
  instanceId=none
else
  >&2 echo "Invalid cloud provider ${cloudProvider}"
  exit 1
fi

jq ".name=\"${distributionName}\" | .title=\"${distributionTitle}\" | .instanceId=\"${instanceId}\" | .mongoDb.name=\"${mongoDbName}\"" config_pattern.json > distribution.json