#!/bin/bash

updateService=installer

. ./update_scripts.sh

distribDirectoryUrl=`jq -r .clientDistributionUrl installer.json`
update_scripts ${distribDirectoryUrl} installer.sh

. ./update.sh "$@"
