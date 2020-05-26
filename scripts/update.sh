#!/bin/bash -e

######## Update utilities script.
# input:
#   $distribDirectoryUrl - distribution URL.
#   $updateService - service to update and run.
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
#  $additionalScripts - additional script files to extract
function updateScripts {
  echo "Check for new version of scripts"
  scriptsVersionFile=.scripts.version
  scriptsDesiredVersion=`getDesiredVersion scripts`
  if [ ! -f ${scriptsVersionFile} ] || [[ "`cat ${scriptsVersionFile}`" != "${scriptsDesiredVersion}" ]]; then
    echo "Download scripts version ${scriptsDesiredVersion}"
    scriptsZipFile=.scripts.zip
    downloadVersionImage scripts ${scriptsDesiredVersion} ${scriptsZipFile}
    scriptFiles="update.sh `basename $0` $additionalScripts"
    echo "Update scripts ${scriptFiles}"
    unzip -qo ${scriptsZipFile} ${scriptFiles}
    chmod +x ${scriptFiles}
    echo ${scriptsDesiredVersion} >${scriptsVersionFile}
    echo "Restart $0"
    exec $0 "$@"
  fi
}

# Updates scripts and service if need. Run service.
# input:
#  $updateService - service
#  $@ - main script arguments
function runService {
  while [ 1 ]
  do
    updateScripts "$@"
    echo "Check for new version of ${updateService}"
    serviceDesiredVersion=`getDesiredVersion ${updateService}`
    if [ ! -f ${updateService}-*.jar ]; then
      update="true"
    else
      currentVersion=`ls ${updateService}-*.jar | sed -e "s/^${updateService}-//; s/\.jar$//" | tail -1`
      if [ "${currentVersion}" != "${serviceDesiredVersion}" ]; then
        echo "Desired version ${serviceDesiredVersion} != current version ${currentVersion}."
        update="true"
      else
        update="false"
      fi
    fi

    if [ "${update}" == "true" ]; then
      echo "Update ${updateService} to version ${serviceDesiredVersion}"
      downloadVersionImage ${updateService} ${serviceDesiredVersion} ${updateService}.zip
      rm -f ${updateService}-*.jar
      unzip -qo ${updateService}.zip && rm -f ${updateService}.zip
    fi

    buildVersion=`echo ${serviceDesiredVersion} | sed -e 's/_.*//'`

    if [ -f install.json ]; then
      command=`jq -r '.runService.command' install.json`
      args=`jq -r '.runService.args | join(" ")' install.json | sed -e s/%%version%%/${buildVersion}/`
    else
      if [ ! -f ${updateService}-${buildVersion}.jar ]; then
        echo "No <${updateService}-${buildVersion}>.jar in the build."
        exit 1
      fi
      command="/usr/bin/java"
      args="-XX:+HeapDumpOnOutOfMemoryError -XX:MaxJavaStackTraceDepth=10000000 -Dscala.control.noTraceSuppression=true -jar ${updateService}-${buildVersion}.jar"
    fi

    echo "Run ${command} ${args} $@"
    ${command} ${args} "$@"

    if [ $? -eq 9 ]; then
      echo "Service ${updateService} is obsoleted. Update it."
    else
      break
    fi
  done
}

if [ ! -z "$updateService" ]; then
  runService "$@"
else
  updateScripts "$@"
fi