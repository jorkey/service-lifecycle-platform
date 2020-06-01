#!/bin/bash -e

serviceToRun=installer

if [ -z "${distribDirectoryUrl}" ]; then
  distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`
fi

. update.sh
