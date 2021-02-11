#!/bin/bash -e
set -e

function createService() {
  echo "Create distribution service"
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
  echo "Start distribution service"

  sudo systemctl daemon-reload
  sudo systemctl restart update-distribution.service
}

createService

exit 0