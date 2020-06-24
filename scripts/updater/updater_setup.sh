#!/bin/bash -e

if [ -z "$4" ]
then
  >&2 echo "Use: $0 cloudProvider name services distribDirectoryUrl"
  exit 1
fi

cloudProvider=$1
name=$2
services=$3
distribDirectoryUrl=$4

serviceToSetup=updater
. update.sh

if [ "${cloudProvider}" = "Azure" ]; then
  instanceId=`sudo dmidecode | grep UUID | awk '{printf $2}'`
else
  >&2 echo "Invalid cloud provider ${cloudProvider}"
  exit 1
fi

cat << EOF > updater.json
{
  "instanceId" : "${instanceId}",
  "clientDistributionUrl" : "${distribDirectoryUrl}"
}
EOF

cat << EOF > updater_pm2.json
{
  "apps" : [{
    "name"            : "${name}",
    "interpreter"     : "none",
    "watch"           : false,
    "script"          : "updater.sh",
    "cwd"             : ".",
    "max_restarts"    : 2147483647,
    "min_uptime"      : "5s",
    "log_date_format" : "YYYY-MM-DD HH:mm:ss.SSS",
    "args": [
      "runServices",
      "services=${services}"
    ]
  }]
}
EOF

pm2 start updater_pm2.json
pm2 save