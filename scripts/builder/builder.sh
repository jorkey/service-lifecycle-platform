#!/bin/bash -e

serviceToRun=builder
## TODO заменить на developerDistributionUrl после перехода на новые API endpoints
distribDirectoryUrl=`jq -r .updateDistributionUrl builder.json`

. update.sh