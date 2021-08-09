#!/bin/bash -e
set -e

function exitUsage() {
  >&2 echo "Use: $0 <cloudProvider> <distribution> <distributionTitle> <mongoDbName> <mongoDbTemporary> <port> <builderDistribution>"
  exit 1
}

if [ $# -ne 7 ]; then
  exitUsage
fi

cloudProvider=$1
distribution=$2
distributionTitle=$3
mongoDbName=$4
mongoDbTemporary=$5
port=$6
builderDistribution=$7

jwtSecret=`openssl rand -base64 32`

if [ "${cloudProvider}" == "Azure" ]; then
  if ! instance=`curl --silent -H "Metadata: True" http://169.254.169.254/metadata/instance?api-version=2019-06-01 | jq -rj '.compute.resourceGroupName, ":", .compute.name'`; then
    >&2 echo "Can't get instance Id"
    exit 1
  fi
elif [ "${cloudProvider}" == "None" ]; then
  instance=none
else
  >&2 echo "Invalid cloud provider ${cloudProvider}"
  exit 1
fi

jq ".distribution=\"${distribution}\" | .title=\"${distributionTitle}\" | .instance=\"${instance}\" | .jwtSecret=\"${jwtSecret}\" | .mongoDb.name=\"${mongoDbName}\" | .mongoDb.temporary=${mongoDbTemporary} | .network.port=${port} | .network.url=\"${url}\" | .builder.distribution=\"${builderDistribution}\"" >distribution.json <<EOF
{
  "distribution": "undefined",
  "title": "undefined",
  "instance": "undefined",
  "jwtSecret": "undefined",
  "mongoDb" : {
    "connection" : "mongodb://localhost:27017",
    "name": "undefined",
    "test": false
  },
  "network": {
    "port" : 8000
  },
  "builder": {
    "distribution": "undefined"
  },
  "versions": {
    "maxHistorySize": 100
  },
  "instanceState": {
    "expirationTimeout": { "length": 60, "unit": "SECONDS" }
  },
  "faultReports": {
    "expirationTimeout": { "length": 7, "unit": "DAYS" },
    "maxReportsCount": 100
  }
}
EOF