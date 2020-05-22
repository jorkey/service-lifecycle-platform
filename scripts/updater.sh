#!/bin/bash -e

updateService=updater
distribDirectoryUrl=`jq -r .clientDistributionUrl updater.json`

. update.sh