#!/bin/bash -e

serviceToRun=installer
distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`

. update.sh
