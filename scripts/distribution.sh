#!/bin/bash

distribDirectory=directory

updateService=distribution
distribDirectoryUrl=file://`/bin/pwd`/${distribDirectory}

echo "Copy desired scripts"

desiredScriptsVersion=`jq -r .desiredVersions.scripts ${distribDirectory}/desired-versions.json`
cp ${distribDirectory}/services/scripts/scripts-${desiredScriptsVersion}.zip scripts.zip || exit 1
unzip -o scripts.zip update.sh || exit 1
rm -f scripts.zip && chmod +x update.sh

. ./update.sh "$@"
