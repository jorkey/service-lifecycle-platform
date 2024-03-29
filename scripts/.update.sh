#!/bin/bash -e
set -e

######## Update utilities script.
# input:
#   $distributionUrl - distribution URL.
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
  if ! http_code=`curl ${url} ${accessToken:+-H Authorization:\ Bearer\ ${accessToken}}  --output ${outputFile} --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`; then
    rm -f ${outputFile}
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

### Graphql request
# input:
#  $1 - source URL
#  $2 - query
#  $3 - sub selection
#  $4 - output file
function graphqlQuery {
  set -e
  local url="$1/graphql"
  local query=$2
  local subSelection=$3
  local outputFile=$4
  rm -f ${outputFile}
  local tmpFile=`mktemp`
  local http_code

  if ! http_code=`curl -X POST ${accessToken:+-H Authorization:\ Bearer\ ${accessToken}} -H "Content-Type: application/json" --data '{ "query": "{ '${query}' }" }' ${url} \
      --output ${tmpFile} --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`; then
    rm -f ${tmpFile}
    exit 1
  elif [[ (${url} == http* && $http_code != "200") || (`jq -r .errors ${tmpFile}` != "null") || (`jq -r .data ${tmpFile}` == "null") ]] ; then
    if [ -f ${tmpFile} ]; then
      >&2 echo "Graphql query: ${query} to url ${url}"
      >&2 echo "Response: `cat ${tmpFile}`"
      rm ${tmpFile}
    fi
    exit 1
  fi
  jq -r .data.${subSelection} ${tmpFile} >${outputFile}
}

## Convert developer version from Json to string
# input:
#   $1 - developer version Json
# output:
#   stdout - short version string
function developerVersionToString {
  echo $1 | jq -r '"\(.distribution)-\((.developerBuild | map(tostring) | join(".")))"'
}

## Convert client version from Json to string
# input:
#   $1 - client version Json
# output:
#   stdout - short version string
function clientVersionToString {
  echo $1 | jq -r '"\(.distribution)-\((.developerBuild | join("."))|tostring)_\(.clientBuild)"'
}

### Get desired version number of service.
# input:
#   $1 - service name
# output:
#   stdout - version number
function getDesiredVersion {
  set -e
  local service=$1
  local storedDesiredVersionFile=`getServiceDesiredVersionFile $1`
  if [ -f ${storedDesiredVersionFile} ]; then
    jq -c . ${storedDesiredVersionFile}
  elif [[ -z ${distributionUrl} ]]; then
    >&2 echo "Variable distributionUrl is not defined"
    exit 1
  elif [[ ${distributionUrl} == http://* ]] || [[ ${distributionUrl} == https://* ]]; then
    local tmpFile=`mktemp`
    graphqlQuery ${distributionUrl} "clientDesiredVersions(services:[\\\"${service}\\\"]){version{distribution,developerBuild,clientBuild}}" "clientDesiredVersions[0].version" ${tmpFile}
    version=`jq -c . ${tmpFile}`
    rm -f ${tmpFile}
    if [[ ${version} == "null" ]]; then
      >&2 echo "Client desired version is not defined for service ${service}"
      exit 1
    fi
    echo ${version}
  else
    >&2 echo "Invalid distribution directory URL ${distributionUrl}"
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
  local version=`clientVersionToString $2`
  local outputFile=$3
  if [[ -z ${distributionUrl} ]]; then
    >&2 echo "Variable distributionUrl is not defined"
    exit 1
  elif [[ ${distributionUrl} == http://* ]] || [[ ${distributionUrl} == https://* ]]; then
    download ${distributionUrl}/load/client-version-image/${service}/${version} ${outputFile}
  else
    >&2 echo "Invalid distribution directory URL ${distributionUrl}"; exit 1
  fi
}

### Get service desired version file.
# input:
#  $1 - service
# output:
#  stdout - desired version file
function getServiceDesiredVersionFile {
  echo ".${1}.desired-version.json"
}

### Get service version file.
# input:
#  $1 - service
# output:
#  stdout - version file
function getServiceVersionFile {
  echo ".${1}.version.json"
}

### Get service version.
# input:
#  $1 - service
# output:
#  stdout - version
function getCurrentVersion {
  set -e
  local currentVersionFile=`getServiceVersionFile $1`
  if [ -f ${currentVersionFile} ]; then
    jq -c . ${currentVersionFile}
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
  local storedDesiredVersionFile=`getServiceVersionFile $1`
  echo $2 >${storedDesiredVersionFile}
  # TODO graphql - update state request
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
  if [[ "${currentVersion}" != "${desiredVersion}" ]]; then
    echo "Download ${service} version `clientVersionToString ${desiredVersion}`"
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
    unzip -qo ${scriptsZipFile} .update.sh
    unzip -qjo ${scriptsZipFile} ${serviceToSetup}/*
    rm -f ${scriptsZipFile}
    chmod +x *.sh .*.sh
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
  while true; do
    set -e
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
    local developerVersion=`developerVersionToString ${currentVersion}`

    if [ -f install.json ]; then
      if ! command=`jq -r '.runService.command' install.json`; then
        >&2 echo "runService.command is not defined in the install.json"
      fi
      if ! args=`jq -r '.runService.args | join(" ")' install.json | sed -e s/%%version%%/${developerVersion}/`; then
        >&2 echo "runService.args is not defined in the install.json"
      fi
    else
      >&2 echo "No install.json"
    fi

    if tty -s; then
      set +e
      echo "Run ${command} ${args} $@"
      ${command} ${args} "$@"
      local status=$?
      set -e
    else
      local child
      function trapKill {
        echo "Termination signal is received. Kill ${serviceToRun}, PID ${child}"
        kill -TERM ${child}
      }
      trap trapKill TERM INT
      set +e
      echo "Run service ${command} ${args} $@"
      ${command} ${args} "$@" &
      child=$!
      wait ${child}
#      trap - TERM INT
#      wait ${child}
      local status=$?
      set -e
      echo "Service ${serviceToRun} is terminated with status ${status}"
    fi
    if [ "$status" == "9" ]; then
      echo "Update and restart ${serviceToRun}"
    else
      exit ${status}
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
