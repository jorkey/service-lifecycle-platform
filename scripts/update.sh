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

### Check scripts version and update if need
# input:
#  $@ - script files to extract
# output:
#  $scriptsUpdated - "true" when scripts are updated
function updateScripts {
  scriptsVersionFile=.scripts.version
  newScriptsVersionFile=.new_scripts.version
  echo "Check for new version of scripts"
  download ${distribDirectoryUrl}/download-desired-version/scripts?image=false ${newScriptsVersionFile}
  if [ ! -f ${scriptsVersionFile} ] || ! diff ${scriptsVersionFile} ${newScriptsVersionFile} >/dev/null; then
    version=`cat ${newScriptsVersionFile}`
    echo "Download scripts version ${version}"
    scriptsZipFile=.scripts.zip
    download ${distribDirectoryUrl}/download-version/scripts/${version} ${scriptsZipFile}
    scriptFiles="update.sh `basename "$0"` $@"
    echo "Update scripts '${scriptFiles}'"
    unzip -qo ${scriptsZipFile} $scriptFiles
    rm -f ${scriptsZipFile}
    chmod +x $scriptFiles
    mv ${newScriptsVersionFile} ${scriptsVersionFile}
    scriptsUpdated="true"
  else
    rm -f ${newScriptsVersionFile}
  fi
}

### Get desired version number of service
# output:
#  $desiredVersion - version number
function getDesiredVersion {
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    desiredVersionFile=.desired-version-${updateService}.json
    download ${distribDirectoryUrl}/download-desired-version/${updateService}?image=false ${desiredVersionFile}
    desiredVersion=`cat ${desiredVersionFile}`
    rm -f ${desiredVersionFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    desiredVersion=`jq -r .desiredVersions.${updateService} ${distribDirectoryUrl}/desired-versions.json`
  else
    echo "Invalid distribution directory URL ${distribDirectoryUrl}"; exit 1
  fi
}

### Download service version image of specified version
# input:
#   $1 - service version
#   $2 - output file
# output:
#  $desiredVersion - version number
function downloadVersionImage {
  version=$1
  outputFile=$2
  if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
    echo "Download version ${version} image"
    download ${distribDirectoryUrl}/download-version/${updateService}/${version} ${outputFile}
  elif [[ ${distribDirectoryUrl} == file://* ]]; then
    echo "Get version ${version} image"
    download ${distribDirectoryUrl}/services/${updateService}/${updateService}-${version}.zip ${outputFile}
  else
    echo "Invalid distribution directory URL ${distribDirectoryUrl}"; exit 1
  fi
}

# Updates scripts and service if need. Run service.
# input:
#  $updateService - service
#  $@ - main script arguments
function runService {
  while [ 1 ]
  do
    updateScripts
    if [ "${scriptsUpdated}" == "true" ]; then
      echo "Restart $0"
      exec $0 "$@"
    fi
    echo "Check for new version of ${updateService}"
    getDesiredVersion
    if [ ! -f ${updateService}-*.jar ]; then
      update="true"
    else
      currentVersion=`ls ${updateService}-*.jar | sed -e "s/^${updateService}-//; s/\.jar$//" | tail -1`
      if [ "${currentVersion}" != "${desiredVersion}" ]; then
        echo "Desired version ${desiredVersion} != current version ${currentVersion}."
        update="true"
      else
        update="false"
      fi
    fi

    if [ "${update}" == "true" ]; then
      echo "Update ${updateService} to version ${desiredVersion}"
      downloadVersionImage ${desiredVersion} ${updateService}.zip
      rm -f ${updateService}-*.jar
      unzip -qo ${updateService}.zip && rm -f ${updateService}.zip
    fi

    buildVersion=`echo ${desiredVersion} | sed -e 's/_.*//'`

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
fi