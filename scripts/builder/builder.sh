#!/bin/bash -e
set -e

serviceToRun=builder
distribDirectoryUrl=`jq -r .updateDistributionUrl builder.json`

. update.sh