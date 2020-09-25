#!/bin/bash -e

if [ -z "$4" ]
then
  >&2 echo "Use: $0 <cloudProvider> <name> <services> <distribDirectoryUrl> [environment]"
  exit 1
fi

cloudProvider=$1
name=$2
services=$3
distribDirectoryUrl=$4
environment=$5

serviceToSetup=updater
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

cat << EOF > updater.json
{
  "instanceId" : "${instanceId}",
  "clientDistributionUrl" : "${distribDirectoryUrl}"
}
EOF

sudo sh -c "cat << EOF > /etc/systemd/system/update-${name}.service
[Unit]
Description=${name}
After=network.target

[Service]
User=ec2-user
KillMode=process
Restart=always
StartLimitInterval=0
LimitCORE=infinity
WorkingDirectory=`pwd`
Environment=${environment}
ExecStart=`pwd`/updater.sh runServices services=${services}

[Install]
WantedBy=multi-user.target
Alias=${name}.service

EOF
"
echo "Service ${name} is created"

sudo systemctl daemon-reload
sudo systemctl restart update-${name}.service