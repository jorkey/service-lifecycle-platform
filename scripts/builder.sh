#!/bin/bash

updateService=builder

. ./update_scripts.sh

distribDirectoryUrl=`jq -r .developerDistributionUrl builder.json`
update_scripts ${distribDirectoryUrl} builder.sh

. ./update.sh "$@"