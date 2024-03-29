#!/bin/bash -e
set -e

function exitUsage {
  >&2 echo "Use: $0 <cloudProvider> <substitutions>"
  exit 1
}

if [ $# -ne 2 ]; then
  exitUsage
fi

cloudProvider=$1
substitutions=$2

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

substitutions="$substitutions | .instance=\"${instance}\" | .jwtSecret=\"${jwtSecret}\""

jq "$substitutions" >distribution.json <<EOF
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
    "host" : "undefined",
    "port" : 8000
  },
  "versions": {
    "maxHistorySize": 25
  },
  "serviceStates": {
    "expirationTimeout": { "length": 60, "unit": "SECONDS" }
  },
  "logs": {
    "serviceLogExpirationTimeout": { "length": 7, "unit": "DAYS" },
    "taskLogExpirationTimeout": { "length": 365, "unit": "DAYS" }
  },
  "faultReports": {
    "expirationTimeout": { "length": 7, "unit": "DAYS" },
    "maxReportsCount": 100
  }
}
EOF