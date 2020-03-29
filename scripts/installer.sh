#!/bin/bash

updateService=installer

#clientName=
#clientDistributionUrl=
#adminRepositoryUrl=

distribDirectoryUrl=http://%%clientName%%:%%clientPassword%%@update.vyulabs.com

echo "Download desired scripts"
curl ${distribDirectoryUrl}/download-desired-version/scripts --output scripts.zip --connect-timeout 5 --silent --show-error || exit 1
unzip -o scripts.zip update.sh || exit 1
rm -f scripts.zip; chmod +x update.sh

if [[ -z $clientName ]]; then
  . ./update.sh "$@" "developerDirectoryUrl=${distribDirectoryUrl}"
else
  . ./update.sh "$@" "developerDirectoryUrl=${distribDirectoryUrl}" "clientName=${clientName}" "clientDistributionUrl=${clientDistributionUrl}" "adminRepositoryUrl=${adminRepositoryUrl}"
fi