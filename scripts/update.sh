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
  url=$1
  outputFile=$2
  rm -f ${outputFile}
  http_code=`curl ${url} --output ${outputFile} --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [[ "${url}" == http* ]] && [ "$http_code" != "200" ]; then
    if [ -f ${url} ]; then
      echo "Request: ${url}"
      echo "Response: `cat ${outputFile}`"
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
  service=$1
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    desiredVersionFile=.desired-version-${service}.json
    download ${distribDirectoryUrl}/download-desired-version/${service}?image=false ${desiredVersionFile}
    cat ${desiredVersionFile}
    rm -f ${desiredVersionFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    desiredVersionsFile=.desired-versions.json
    download ${distribDirectoryUrl}/desired-versions.json ${desiredVersionsFile}
    jq -r .desiredVersions.${service} ${desiredVersionsFile}
    rm -f ${desiredVersionsFile}
  else
    echo "Invalid distribution directory URL ${distribDirectoryUrl}"
    exit 1
  fi
}

### Download service version image of specified version.
# input:
#  $1 - service name
#  $2 - service version
#  $3 - output file
function downloadVersionImage {
  service=$1
  version=$2
  outputFile=$3
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    echo "Download version ${version} image of service ${service}"
    download ${distribDirectoryUrl}/download-version/${service}/${version} ${outputFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    echo "Get version ${version} image of service ${service}"
    download ${distribDirectoryUrl}/services/${service}/${service}-${version}.zip ${outputFile}
  else
    echo "Invalid distribution directory URL ${distribDirectoryUrl}"; exit 1
  fi
}

### Check scripts version. Update and restart if need.
# input:
#  $serviceToSetup - service to extract script files.
function updateScripts {
  echo "Check for new version of scripts"
  scriptsVersionFile=.scripts.version
  scriptsZipFile=.scripts.zip
  scriptsDesiredVersion=`getDesiredVersion scripts`
  if [ ! -f ${scriptsVersionFile} ] || [[ "`cat ${scriptsVersionFile}`" != "${scriptsDesiredVersion}" ]]; then
    echo "Download scripts version ${scriptsDesiredVersion}"
    downloadVersionImage scripts ${scriptsDesiredVersion} ${scriptsZipFile}
    echo "Update scripts"
    unzip -qo ${scriptsZipFile} update.sh
    unzip -qjo ${scriptsZipFile} ${serviceToSetup}/*
    chmod +x *.sh
    echo ${scriptsDesiredVersion} >${scriptsVersionFile}
    echo "Restart $0"
    exec $0 "$@"
  else
    # TODO remove when ported to scripts groups
    unzip -qjf ${scriptsZipFile} ${serviceToSetup}/*
    chmod +x *.sh
  fi
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
    echo "Check for new version of ${serviceToRun}"
    serviceDesiredVersion=`getDesiredVersion ${serviceToRun}`
    if [ ! -f ${serviceToRun}-*.jar ]; then
      update="true"
    else
      currentVersion=`ls ${serviceToRun}-*.jar | sed -e "s/^${serviceToRun}-//; s/\.jar$//" | tail -1`
      if [ "${currentVersion}" != "${serviceDesiredVersion}" ]; then
        echo "Desired version ${serviceDesiredVersion} != current version ${currentVersion}."
        update="true"
      else
        update="false"
      fi
    fi

    if [ "${update}" == "true" ]; then
      echo "Update ${serviceToRun} to version ${serviceDesiredVersion}"
      downloadVersionImage ${serviceToRun} ${serviceDesiredVersion} ${serviceToRun}.zip
      rm -f ${serviceToRun}-*.jar
      unzip -qo ${serviceToRun}.zip && rm -f ${serviceToRun}.zip
    fi

    buildVersion=`echo ${serviceDesiredVersion} | sed -e 's/_.*//'`

    if [ -f install.json ]; then
      command=`jq -r '.runService.command' install.json`
      args=`jq -r '.runService.args | join(" ")' install.json | sed -e s/%%version%%/${buildVersion}/`
    else
      if [ ! -f ${serviceToRun}-${buildVersion}.jar ]; then
        echo "No <${serviceToRun}-${buildVersion}>.jar in the build."
        exit 1
      fi
      command="/usr/bin/java"
      args="-XX:+HeapDumpOnOutOfMemoryError -XX:MaxJavaStackTraceDepth=10000000 -Dscala.control.noTraceSuppression=true -jar ${serviceToRun}-${buildVersion}.jar"
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

if [ ! -z "${serviceToRun}" ]; then
  serviceToSetup=${serviceToRun}
  runService "$@"
elif [ ! -z "${serviceToSetup}" ]; then
  updateScripts "$@"
fi