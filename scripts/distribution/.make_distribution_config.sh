#!/bin/bash -e
set -e

function exitUsage {
  >&2 echo "Use: $0 <cloudProvider> <distribution> <distributionTitle> "\
    "<mongoDbConnection> <mongoDbName> <mongoDbTemporary> <port> <builderDistribution>"
  exit 1
}

if [ $# -ne 8 ]; then
  exitUsage
fi

cloudProvider=$1
distribution=$2
distributionTitle=$3
mongoDbConnection=$4
mongoDbName=$5
mongoDbTemporary=$6
port=$7
builderDistribution=$8

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

jq ".distribution=\"${distribution}\" | .title=\"${distributionTitle}\" | .instance=\"${instance}\" | .jwtSecret=\"${jwtSecret}\" | .mongoDb.connection=\"${mongoDbConnection}\" | .mongoDb.name=\"${mongoDbName}\" | .mongoDb.temporary=${mongoDbTemporary} | .network.port=${port} | .builder.distribution=\"${builderDistribution}\"" >distribution.json <<EOF
{
  "distribution": "undefined",
  "title": "undefined",
  "instance": "undefined",
  "jwtSecret": "undefined",
  "mongoDb" : {
    "connection" : "undefined",
    "name": "undefined"
  },
  "network": {
    "port" : 8000
  },
  "builder": {
    "distribution": "undefined"
  },
  "versions": {
    "maxHistorySize": 25
  },
  "serviceStates": {
    "expirationTimeout": { "length": 60, "unit": "SECONDS" }
  },
  "logs": {
    "expirationTimeout": { "length": 7, "unit": "DAYS" }
  },
  "faultReports": {
    "expirationTimeout": { "length": 7, "unit": "DAYS" },
    "maxReportsCount": 100
  }
}
EOF