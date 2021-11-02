#!/bin/bash -e
set -e

if [ -z "$4" ]
then
  >&2 echo "Use: $0 <cloudProvider> <name> <services> <distributionUrl> <accessToken> [environment]"
  exit 1
fi

cloudProvider=$1
name=$2
services=$3
distributionUrl=$4
accessToken=$5
environment=$6

serviceToSetup=updater
. .update.sh

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
  "distributionUrl" : "${distributionUrl}",
  "accessToken" : "${accessToken}"
}
EOF

sudo -p "Enter 'sudo' password:" sh -c "cat << EOF > /etc/systemd/system/update-${name}.service
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

sudo -p "Enter 'sudo' password:" systemctl daemon-reload
sudo -p "Enter 'sudo' password:" systemctl reenable update-${name}.service
sudo -p "Enter 'sudo' password:" systemctl restart update-${name}.service