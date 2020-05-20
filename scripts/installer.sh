#!/bin/bash -e

updateService=installer

distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`

. ./update.sh "$@"
