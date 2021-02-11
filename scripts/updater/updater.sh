#!/bin/bash -e
set -e

serviceToRun=updater
distribDirectoryUrl=`jq -r .clientDistributionUrl updater.json`

. update.sh