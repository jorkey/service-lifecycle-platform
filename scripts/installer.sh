#!/bin/bash -e

serviceToRun=installer
scriptsToUpdate="installer.sh update.sh"
distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`

. update.sh
