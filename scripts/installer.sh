#!/bin/bash

updateService=installer

distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`

. ./update.sh "$@"
