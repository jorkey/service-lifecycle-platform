#!/bin/bash -e
set -e

serviceToRun=builder
distribDirectoryUrl=`jq -r .distributionLinks[0].distributionUrl builder.json`

. .update.sh