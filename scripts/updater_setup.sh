#!/bin/bash -e

if [ -z "$4" ]
then
  echo "Use: $0 cloudProvider name services distribDirectoryUrl"
  exit 1
fi

cloudProvider=$1
name=$2
services=$3
distribDirectoryUrl=$4

updateService=updater

if [ "${cloudProvider}" = "Azure" ]; then
  instanceId=`sudo dmidecode | grep UUID | awk '{printf $2}'`
else
  echo "Invalid cloud provider ${cloudProvider}"
  exit 1
fi

escapedDisribDirectoryUrl=`echo ${distribDirectoryUrl} | sed -e 's/\\//\\\\\//g'`

cat << EOF > updater_pm2.json
{
  "apps" : [{
    "name"         : "${name}",
    "interpreter"  : "none",
    "watch"        : false,
    "script"       : "updater.sh",
    "cwd"          : ".",
    "max_restarts" : 2147483647,
    "min_uptime"   : "5s",
    "merge_logs"   : true,
    "args": [
      "${instanceId}",
      "${services}",
      "${escapedDisribDirectoryUrl}"
    ]
  }]
}
EOF

unzip -o scripts.zip updater.sh update_scripts.sh && chmod +x updater.sh update_scripts.sh

pm2 start updater_pm2.json
pm2 save