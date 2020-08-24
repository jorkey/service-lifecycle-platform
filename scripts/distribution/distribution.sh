#!/bin/bash -e

serviceToRun=distribution
distribDirectoryUrl=`jq -r .distribDirectoryUrl distribution.json`

. update.sh