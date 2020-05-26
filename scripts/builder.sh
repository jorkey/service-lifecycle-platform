#!/bin/bash -e

serviceToRun=builder
scriptsToUpdate="builder.sh update.sh"
distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`

. update.sh