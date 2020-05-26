#!/bin/bash -e

serviceToRun=distribution
scriptsToUpdate="distribution_setup.sh distribution.sh update.sh"
distribDirectoryUrl=file://`/bin/pwd`/directory

. update.sh