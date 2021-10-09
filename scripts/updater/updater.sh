#!/bin/bash -e
set -e

serviceToRun=updater
distributionUrl=`jq -r .distributionUrl updater.json`
accessToken=`jq -r .accessToken updater.json`

. .update.sh