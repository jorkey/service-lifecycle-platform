#!/bin/bash -e

function exitUsage() {
  >&2 echo "Use: $0 developer <cloudProvider> <name> <port> or"
  >&2 echo "     $0 client <cloudProvider> <name> <port> <developerDirectoryUrl>"
  exit 1
}

if [ "$1" == "developer" ]; then
  if [ -z "$4" ]; then
    exitUsage
  fi
  distribDirectoryUrl=file://`/bin/pwd`/directory
elif [ "$1" == "client" ]; then
  if [ -z "$5" ]; then
    exitUsage
  fi
  distribDirectoryUrl=$5
else
  exitUsage
fi

role=$1
cloudProvider=$2
name=$3
port=$4

serviceToSetup=distribution
. update.sh

if [ "${cloudProvider}" == "Azure" ]; then
  if ! instanceId=`curl --silent -H "Metadata: True" http://169.254.169.254/metadata/instance?api-version=2019-06-01 | jq -rj '.compute.resourceGroupName, ":", .compute.name'`; then
    >&2 echo "Can't get instance Id"
    exit 1
  fi
else
  >&2 echo "Invalid cloud provider ${cloudProvider}"
  exit 1
fi

function createService() {
  sudo sh -c "cat << EOF > /etc/systemd/system/update-distribution.service
[Unit]
Description=Update distribution server
After=network.target

[Service]
User=ec2-user
KillMode=process
Restart=always
RestartSec=0
StartLimitInterval=0
LimitCORE=infinity
WorkingDirectory=`pwd`
ExecStart=`pwd`/distribution.sh "$@"

[Install]
WantedBy=multi-user.target
Alias=distribution.service

EOF
"
  echo "Service distribution is created"

  sudo systemctl daemon-reload
  sudo systemctl restart update-distribution.service
}

if [ "$role" == "developer" ]; then
  cat << EOF > distribution.json
{
  "name": "${name}",
  "instanceId": "${instanceId}",
  "port": ${port},
  "distributionDirectory": "directory",
  "builderDirectory": "../build"
}
EOF

  createService developer

elif [ "$role" == "client" ]; then
  cat << EOF > distribution.json
{
  "name": "${name}",
  "instanceId": "${instanceId}",
  "port": ${port},
  "distributionDirectory": "directory",
  "installerDirectory": "../install",
  "developerDistributionUrl" : "${distribDirectoryUrl}"
}
EOF

  createService client

else
  exitUsage
fi

exit 0