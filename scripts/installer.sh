#!/bin/bash

updateService=installer

. ./update_scripts.sh

distribDirectoryUrl=`jq -r .developerDistributionUrl installer.json`
update_scripts ${distribDirectoryUrl} installer.sh

. ./update.sh "$@"
