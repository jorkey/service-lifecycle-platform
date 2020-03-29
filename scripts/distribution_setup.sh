#!/bin/bash

function exitUsage() {
  echo "Use: $0 developer <port> or"
  echo "     $0 client <clientName> <developerDirectoryUrl> <port>"
  exit 1
}

if [ -z "$1" ]
then
  exitUsage
fi

if [ "$1" = "developer" ]; then
  if [ -z "$2" ]; then
    exitUsage
  fi
  port=$2
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
      "developer",
      "port=${port}"
    ]
  }]
}
EOF
elif [ "$1" = "client" ]; then
  if [ -z "$4" ]; then
    exitUsage
  fi
  clientName=$2
  distribDirectoryUrl=$3
  port=$4
  escapedDisribDirectoryUrl=`echo ${distribDirectoryUrl} | sed -e 's/\\//\\\\\//g'`
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
      "client",
      "port=${port}",
      "clientName=${clientName}",
      "developerDirectoryUrl=${escapedDisribDirectoryUrl}"
    ]
  }]
}
EOF
else
  exitUsage
fi

echo "distribution_pm2.json is created"

chmod +x distribution.sh || exit 1
pm2 start distribution_pm2.json || exit 1

exit 0