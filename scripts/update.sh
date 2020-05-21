#!/bin/bash -e

######## Update utilities. Input parameters: $distribDirectoryUrl - distribution URL.

### Download resource to file
# Input:
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
# Input:
#  $@ - script files to extract
# Output:
#  $scriptsUpdated - "true" when scripts are updated
function updateScripts {
  scriptsVersionFile=.scripts.version
  newScriptsVersionFile=.new_scripts.version
  echo "Check scripts version"
  download ${distribDirectoryUrl}/download-desired-version/scripts?image=false ${newScriptsVersionFile}
  if [ ! -f ${scriptsVersionFile} ] || ! diff ${scriptsVersionFile} ${newScriptsVersionFile} >/dev/null; then
    version=`cat ${newScriptsVersionFile}`
    echo "Download scripts version ${version}"
    scriptsZipFile=.scripts.zip
    download ${distribDirectoryUrl}/download-version/scripts/${version} ${scriptsZipFile}
    echo "Update scripts"
    scriptFiles="update.sh `basename "$0"` $@"
    unzip -qo ${scriptsZipFile} $scriptFiles
    rm -f ${scriptsZipFile}
    chmod +x $scriptFiles
    mv ${newScriptsVersionFile} ${scriptsVersionFile}
    scriptsUpdated="true"
  else
    rm -f ${newScriptsVersionFile}
  fi
}

if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
  function getDesiredVersion {
    echo "Get desired version for service ${updateService}"
    desiredVersionFile=.desired-version-${updateService}.json
    download ${distribDirectoryUrl}/download-desired-version/${updateService}?image=false ${desiredVersionFile}
    desiredVersion=`cat ${desiredVersionFile}`
    rm -f ${desiredVersionFile}
  }
  function downloadVersionImage {
    echo "Download version ${1} image"
    download ${distribDirectoryUrl}/download-version/${updateService}/$1 $2
  }
elif [[ ${distribDirectoryUrl} == file://* ]]; then
  function getDesiredVersion {
    echo "Get desired version for service ${updateService}"
    desiredVersion=`jq -r .desiredVersions.${updateService} ${distribDirectoryUrl}/desired-versions.json`
  }
  function downloadVersionImage {
    echo "Get version ${1} image"
    download ${distribDirectoryUrl}/services/${updateService}/${updateService}-$1.zip $2
  }
else
  echo "Invalid distribution directory URL ${distribDirectoryUrl}"
  exit 1
fi

# Updates scripts and service if need. Run service.
# Input:
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
fi
