#!/bin/bash -e

function exitUsage() {
  >&2 echo "Use: $0 developer <name> <port> or"
  >&2 echo "     $0 client <name> <port> <developerDirectoryUrl>"
  exit 1
}

if [ "$1" == "developer" ]; then
  if [ -z "$3" ]; then
    exitUsage
  fi
  distribDirectoryUrl=file://`/bin/pwd`/directory
elif [ "$1" == "client" ]; then
  if [ -z "$4" ]; then
    exitUsage
  fi
  distribDirectoryUrl=$4
else
  exitUsage
fi

serviceToSetup=distribution
. update.sh

function createService() {
  sudo sh -c "cat << EOF > /etc/systemd/system/update-distribution.service
[Unit]
Description=Update distribution server

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
Alias=distribution.service

EOF
"
  echo "Service distribution is created"

  sudo systemctl daemon-reload
  sudo systemctl restart update-distribution.service
}

if [ "$1" == "developer" ]; then
  if [ -z "$2" ]; then
    exitUsage
  fi
  name=$2
  port=$3
  cat << EOF > distribution.json
{
  "name: " ${name},
  "port" : ${port}
}
EOF

  createService developer

elif [ "$1" == "client" ]; then
  if [ -z "$3" ]; then
    exitUsage
  fi
  name=$2
  port=$3
  distribDirectoryUrl=$4
  cat << EOF > distribution.json
{
  "name: " ${name},
  "port" : ${port},
  "developerDistributionUrl" : "${distribDirectoryUrl}"
}
EOF

  createService client

else
  exitUsage
fi

exit 0