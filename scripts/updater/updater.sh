#!/bin/bash -e

serviceToRun=updater
distribDirectoryUrl=`jq -r .clientDistributionUrl updater.json`

. update.sh