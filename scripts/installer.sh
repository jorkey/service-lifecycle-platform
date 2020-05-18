#!/bin/bash

updateService=installer

. ./update_scripts.sh

distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`
update_scripts ${distribDirectoryUrl} installer.sh

. ./update.sh "$@"
