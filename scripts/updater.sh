#!/bin/bash -e

serviceToRun=updater
distribDirectoryUrl=`jq -r .clientDistributionUrl updater.json`
scriptsToUpdate="updater_setup.sh updater.sh update.sh"

. update.sh