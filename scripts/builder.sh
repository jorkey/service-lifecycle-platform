#!/bin/bash

updateService=builder

echo "Download desired scripts"
curl ${distribDirectoryUrl}/download-desired-version/scripts --output scripts.zip --connect-timeout 5 --silent --show-error || exit 1
unzip -o scripts.zip update.sh && rm -f scripts.zip && chmod +x update.sh || exit 1

. ./update.sh "$@"
