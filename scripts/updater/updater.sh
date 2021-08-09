#!/bin/bash -e
set -e

serviceToRun=updater
distributionUrl=`jq -r .clientDistributionUrl updater.json`

. .update.sh