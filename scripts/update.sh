#!/bin/bash

echo "Start update"

function download {
  rm -f $2
  http_code=`curl $1 --output $2 --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [ "$1" == "http://*" ] && [ "$http_code" != "200" ]; then
    if [ -f $2 ]; then
      echo "Server returned: "; cat $2; rm $2; echo
    fi
    exit 1
  fi
}

if [[ ${distribDirectoryUrl} == http://* ]] || [[ ${distribDirectoryUrl} == https://* ]]; then
  function downloadDesiredVersions {
    echo "Download desired versions"
    download ${distribDirectoryUrl}/download-desired-versions $1
  }
  function downloadVersionImage {
    echo "Download version ${1} image"
    download ${distribDirectoryUrl}/download-version/${updateService}/$1 $2
  }
elif [[ ${distribDirectoryUrl} == file://* ]]; then
  function downloadDesiredVersions {
    download ${distribDirectoryUrl}/desired-versions.json $1
  }
  function downloadVersionImage {
    download ${distribDirectoryUrl}/services/${updateService}/${updateService}-$1.zip $2
  }
else
  echo "Invalid distribution directory URL ${distribDirectoryUrl}"
  exit 1
fi

while [ 1 ]
do
  downloadDesiredVersions .desired-versions.json
  desiredVersion=`jq -r .desiredVersions.${updateService} .desired-versions.json`

  echo "Check for new version of ${updateService}"
  if [ ! -f ${updateService}-*.jar ]; then
    update="true"
  else
    currentVersion=`ls ${updateService}-*.jar | sed -e "s/^${updateService}-//; s/\.jar$//" | tail -1`
    if [ "${currentVersion}" != "${desiredVersion}" ]; then
      echo "Desired version ${desiredVersion} != current version ${currentVersion}."
      update="true"
    else
      rm -f .desired-versions.json
      update="false"
    fi
  fi

  if [ "${update}" = "true" ]; then
    echo "Update ${updateService} to version ${desiredVersion}"
    downloadVersionImage ${desiredVersion} ${updateService}.zip
    rm -f ${updateService}-*.jar || exit 1
    unzip -o ${updateService}.zip && rm -f ${updateService}.zip || exit 1
    rm -f .desired-versions.json
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
