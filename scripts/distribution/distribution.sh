#!/bin/bash -e

serviceToRun=distribution
selfDistributionClient=`jq -r .selfDistributionClient distribution.json`
if [ "${selfDistributionClient}" != "null" ]; then
  selfDistributionDirectory="directory/clients/${selfDistributionClient}"
else
  selfDistributionDirectory=`jq -r .distributionDirectory distribution.json`
fi
distribDirectoryUrl="file://`/bin/pwd`/${selfDistributionDirectory}"

. update.sh