#!/bin/bash -e

updateService=installer
distribDirectoryUrl=`jq -r .developerDistributionUrl installer.json`

. ./update.sh

runService "$@"
