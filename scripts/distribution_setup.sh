#!/bin/bash -e

function exitUsage() {
  echo "Use: $0 developer <port> or"
  echo "     $0 client <port> <developerDirectoryUrl>"
  exit 1
}

if [ -z "$1" ]
then
  exitUsage
fi

if [ "$1" == "developer" ]; then
  distribDirectoryUrl=file://`/bin/pwd`/directory
elif [ "$1" == "client" ]; then
  if [ -z "$3" ]; then
    exitUsage
  fi
  distribDirectoryUrl=$3
else
  exitUsage
fi

additionalScripts=distribution.sh
. update.sh

if [ "$1" == "developer" ]; then
  if [ -z "$2" ]; then
    exitUsage
  fi
  port=$2
  cat << EOF > distribution.json
{
  "port=${port}"
}
EOF
  cat << EOF > distribution_pm2.json
{
  "apps" : [{
    "name"         : "distribution",
    "interpreter"  : "none",
    "watch"        : false,
    "script"       : "distribution.sh",
    "max_restarts" : 2147483647,
    "min_uptime"   : "5s",
    "merge_logs"   : true,
    "cwd"          : ".",
    "args": [
      "developer"
    ]
  }]
}
EOF
elif [ "$1" == "client" ]; then
  if [ -z "$3" ]; then
    exitUsage
  fi
  port=$2
  distribDirectoryUrl=$3
  cat << EOF > distribution.json
{
  "port=${port}",
  "developerDistributionUrl=${distribDirectoryUrl}"
}
EOF
  cat << EOF > distribution_pm2.json
{
  "apps" : [{
    "name"        : "distribution",
    "interpreter" : "none",
    "watch"       : false,
    "script"      : "distribution.sh",
    "cwd"         : ".",
    "merge_logs"  : true,
    "args": [
      "client"
    ]
  }]
}
EOF
else
  exitUsage
fi

echo "distribution_pm2.json is created"

chmod +x distribution.sh
pm2 start distribution_pm2.json

exit 0