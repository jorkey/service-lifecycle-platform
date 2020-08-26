#!/bin/bash -e

serviceToRun=distribution
selfDistributionDirectory=`jq -r .selfDistributionDirectory distribution.json`
if [ "${selfDistributionDirectory}" == "null" ]; then
  selfDistributionDirectory=`jq -r .distributionDirectory distribution.json`
fi
distribDirectoryUrl="file://`/bin/pwd`/${selfDistributionDirectory}"

. update.sh