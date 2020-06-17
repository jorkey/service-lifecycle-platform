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
  local url=$1
  local outputFile=$2
  rm -f ${outputFile}
  http_code=`curl ${url} --output ${outputFile} --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [[ "${url}" == http* ]] && [ "$http_code" != "200" ]; then
    if [ -f ${url} ]; then
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
  local service=$1
  local version=$2
  local outputFile=$3
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    >&2 echo "Download version ${version} image of service ${service}"
    download ${distribDirectoryUrl}/download-version/${service}/${version} ${outputFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    >&2 echo "Get version ${version} image of service ${service}"
    download ${distribDirectoryUrl}/services/${service}/${service}-${version}.zip ${outputFile}
  else
    >&2 echo "Invalid distribution directory URL ${distribDirectoryUrl}"; exit 1
  fi
}

### Check scripts version. Update and restart if need.
# input:
#  $@ - external arguments
#  $serviceToSetup - service to extract script files
function updateScripts {
  local scriptsZipFile=.scripts.zip
  local newVersion=`updateService scripts ${scriptsZipFile}`
  if [ -f ${scriptsZipFile} ]; then
    >&2 echo "Update scripts"
    unzip -qo ${scriptsZipFile} update.sh
    unzip -qjo ${scriptsZipFile} ${serviceToSetup}/*
    rm -f ${scriptsZipFile}
    chmod +x *.sh
    >&2 echo "Restart $0"
    exec $0 "$@"
  fi
}

### Check service version. Update if need.
# input:
#  $1 - service to update
# output:
#  new version
function updateJavaService {
  local service=$1
  local serviceZipFile=.${service}.zip
  local newVersion=`updateService ${service} ${serviceZipFile}`
  if [ -f ${serviceZipFile} ]; then
    rm -f ${service}-*.jar
    unzip -qo ${serviceZipFile}
    rm -f ${serviceZipFile}
  fi
  echo ${newVersion}
}

### Check service version. Update if need.
# input:
#  $1 - service to update
#  $2 - download file
# output:
#  new version
function updateService {
  local service=$1
  local outputFile=$2
  >&2 echo "Check for new version of ${service}"
  local serviceVersionFile=.${service}.version
  local serviceDesiredVersion=`getDesiredVersion ${service}`
  if [ ! -f ${serviceVersionFile} ] || [[ "`cat ${serviceVersionFile}`" != "${serviceDesiredVersion}" ]]; then
    >&2 echo "Download service version ${serviceDesiredVersion}"
    downloadVersionImage ${service} ${serviceDesiredVersion} ${outputFile}
    echo ${serviceDesiredVersion} >${serviceVersionFile}
  fi
  echo ${serviceDesiredVersion}
}

# Updates scripts and service if need. Run service.
# input:
#  $serviceToRun - service to update and run
#  $@ - main script arguments
function runService {
  while [ 1 ]
  do
    serviceToSetup=${serviceToRun}
    updateScripts "$@"

    local buildVersion=`updateJavaService ${serviceToRun} | sed -e 's/_.*//'`

    if [ -f install.json ]; then
      command=`jq -r '.runService.command' install.json`
      args=`jq -r '.runService.args | join(" ")' install.json | sed -e s/%%version%%/${buildVersion}/`
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
      >&2 echo "Service ${serviceToRun} is obsoleted. Update it."
    else
      break
    fi
  done
}

if [ ! -z "${serviceToRun}" ]; then
  serviceToSetup=${serviceToRun}
  runService "$@"
elif [ ! -z "${serviceToSetup}" ]; then
  updateScripts "$@"
else
  >&2 echo "Variable serviceToRun or serviceToSetup must be set"
  exit 1
fi