#!/bin/bash

updateService=builder

distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`

. ./update.sh "$@"