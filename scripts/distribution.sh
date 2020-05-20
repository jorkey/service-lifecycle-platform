#!/bin/bash

updateService=distribution

distribDirectory=directory
distribDirectoryUrl=file://`/bin/pwd`/${distribDirectory}

. ./update.sh "$@"
