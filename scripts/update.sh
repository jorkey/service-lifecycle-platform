#!/bin/bash -e

######## Update utilities script.
# input:
#   $distribDirectoryUrl - distribution URL.
#   $serviceToRun - service to update and run.
#   $serviceToSetup - service to setup.
########

### Download resource to file
# input:
#  $1 - source URL
#  $2 - output file
function download {
  set -e
  local url=$1
  local outputFile=$2
  rm -f ${outputFile}
  local http_code
  if ! http_code=`curl ${url} --output ${outputFile} --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`; then
    exit 1
  elif [[ ${url} == http* ]] && [[ $http_code != "200" ]]; then
    if [ -f ${outputFile} ]; then
      >&2 echo "Request: ${url}"
      >&2 echo "Response: `cat ${outputFile}`"
      rm ${outputFile}
    fi
    exit 1
  fi
}

### Get desired version number of service.
# input:
#   $1 - service name
# output:
#   stdout - version number
function getDesiredVersion {
  set -e
  local service=$1
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    local desiredVersionFile=.desired-version-${service}.json
    download ${distribDirectoryUrl}/download-desired-version/${service}?image=false ${desiredVersionFile}
    cat ${desiredVersionFile}
    rm -f ${desiredVersionFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    local desiredVersionsFile=.desired-versions.json
    download ${distribDirectoryUrl}/desired-versions.json ${desiredVersionsFile}
    jq -r .desiredVersions.${service} ${desiredVersionsFile}
    rm -f ${desiredVersionsFile}
  else
    >&2 echo "Invalid distribution directory URL ${distribDirectoryUrl}"
    exit 1
  fi
}

### Download service version image of specified version.
# input:
#  $1 - service name
#  $2 - service version
#  $3 - output file
function downloadVersionImage {
  set -e
  local service=$1
  local version=$2
  local outputFile=$3
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    download ${distribDirectoryUrl}/download-version/${service}/${version} ${outputFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    download ${distribDirectoryUrl}/services/${service}/${service}-${version}.zip ${outputFile}
  else
    >&2 echo "Invalid distribution directory URL ${distribDirectoryUrl}"; exit 1
  fi
}

### Get service version file.
# input:
#  $1 - service
# output:
#  stdout - version file
function getServiceVersionFile {
  echo ".${1}.version"
}

### Get service version.
# input:
#  $1 - service
# output:
#  stdout - version
function getCurrentVersion {
  set -e
  local versionFile=`getServiceVersionFile $1`
  if [ -f ${versionFile} ]; then
    cat ${versionFile}
  else
    echo
  fi
}

### Set service version.
# input:
#  $1 - service
#  $2 - version
# output:
#  stdout - version
function setCurrentVersion {
  set -e
  local versionFile=`getServiceVersionFile $1`
  echo $2 >${versionFile}
}

### Check service version. Update if need.
# input:
#  $1 - service to update
#  $2 - download file
function updateService {
  set -e
  local service=$1
  local outputFile=$2
  echo "Check for current and desired version of ${service}"
  local currentVersion
  if ! currentVersion=`getCurrentVersion ${service}`; then
    >&2 echo "Getting current version of ${service} error"
    exit 1
  fi
  local desiredVersion
  if ! desiredVersion=`getDesiredVersion ${service}`; then
    >&2 echo "Getting desired version of ${service} error"
    exit 1
  fi
  if [[ ${currentVersion} != ${desiredVersion} ]]; then
    echo "Download ${service} version ${desiredVersion}"
    downloadVersionImage ${service} ${desiredVersion} ${outputFile}
    setCurrentVersion ${service} ${desiredVersion}
  fi
}

### Check scripts version. Update and restart if need.
# input:
#  $@ - external arguments
#  $serviceToSetup - service to extract script files
function updateScripts {
  set -e
  local scriptsZipFile=.scripts.zip
  if ! updateService scripts ${scriptsZipFile}; then
    >&2 echo "Update scripts error"
    exit 1
  fi
  if [ -f ${scriptsZipFile} ]; then
    echo "Update scripts"
    unzip -qo ${scriptsZipFile} update.sh
    unzip -qjo ${scriptsZipFile} ${serviceToSetup}/*
    rm -f ${scriptsZipFile}
    chmod +x *.sh
    echo "Restart $0"
    exec $0 "$@"
  fi
}

### Check service version. Update if need.
# input:
#  $1 - service to update
function updateJavaService {
  set -e
  local service=$1
  local serviceZipFile=.${service}.zip
  if ! updateService ${service} ${serviceZipFile}; then
    >&2 echo "Update ${service} error"
    exit 1
  fi
  if [ -f ${serviceZipFile} ]; then
    echo "Update ${service}"
    rm -f ${service}-*.jar
    unzip -qo ${serviceZipFile}
    rm -f ${serviceZipFile}
  fi
}

# Updates scripts and service if need. Run service.
# input:
#  $serviceToRun - service to update and run
#  $@ - main script arguments
function runService {
  set -e
  while [ 1 ]
  do
    serviceToSetup=${serviceToRun}
    updateScripts "$@"

    if ! updateJavaService ${serviceToRun}; then
      exit 1
    fi
    local currentVersion
    if ! currentVersion=`getCurrentVersion ${serviceToRun}`; then
      >&2 echo "Getting current version of ${serviceToRun} error"
      exit 1
    fi
    local buildVersion=`${currentVersion} | sed -e 's/_.*//'`

    if [ -f install.json ]; then
      if ! command=`jq -r '.runService.command' install.json`; then
        >&2 echo "runService.command is not defined in the install.json"
      fi
      if ! args=`jq -r '.runService.args | join(" ")' install.json | sed -e s/%%version%%/${buildVersion}/`; then
        >&2 echo "runService.args is not defined in the install.json"
      fi
    else
      if [ ! -f ${serviceToRun}-${buildVersion}.jar ]; then
        >&2 echo "No <${serviceToRun}-${buildVersion}>.jar in the build."
        exit 1
      fi
      command="/usr/bin/java"
      args="-jar ${serviceToRun}-${buildVersion}.jar"
    fi

    echo "Run ${command} ${args} $@"
    ${command} ${args} "$@"

    if [ $? -eq 9 ]; then
      echo "Service ${serviceToRun} is obsoleted. Update it."
    else
      break
    fi
  done
}

if [[ ! -z ${serviceToRun} ]]; then
  serviceToSetup=${serviceToRun}
  runService "$@"
elif [[ ! -z ${serviceToSetup} ]]; then
  updateScripts "$@"
else
  >&2 echo "Variable serviceToRun or serviceToSetup must be set"
  exit 1
fi