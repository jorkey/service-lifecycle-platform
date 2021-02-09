#!/bin/bash -e

function exitUsage() {
  >&2 echo "Use: $0 <cloudProvider>"
  exit 1
}

cloudProvider=$1

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

createService

exit 0