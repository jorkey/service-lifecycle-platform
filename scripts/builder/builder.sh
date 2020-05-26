#!/bin/bash -e

serviceToRun=builder
distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`

. update.sh