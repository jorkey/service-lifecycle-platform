#!/bin/bash -e

if [ -z "$4" ]
then
  >&2 echo "Use: $0 cloudProvider name services distribDirectoryUrl [environment]"
  exit 1
fi

cloudProvider=$1
name=$2
services=$3
distribDirectoryUrl=$4
environment=$5

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

sudo sh -c "cat << EOF > /etc/systemd/system/update-${name}.service
[Unit]
Description=${name}

[Service]
User=ec2-user
KillMode=process
Restart=on-failure
RestartSec=1s
WorkingDirectory=`pwd`
Environment=${environment}
ExecStart=`pwd`/updater.sh runServices services=${services}

[Install]
Alias=${name}.service

EOF
"
echo "Service ${name} is created"

sudo systemctl daemon-reload
sudo systemctl start update-${name}.service