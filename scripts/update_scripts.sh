#!/bin/bash -e

function download {
  rm -f $2
  http_code=`curl $1 --output $2 --write-out "%{http_code}" --connect-timeout 5 --silent --show-error`
  if [ "$?" != "0" ]; then
    exit 1
  elif [[ "$1" == http* ]] && [ "$http_code" != "200" ]; then
    if [ -f $2 ]; then
      echo "Request: ${1}"
      echo "Response: `cat $2`"
      rm $2
    fi
    exit 1
  fi
}

function updateScripts {
  scriptsVersionFile=.scripts.version
  newScriptsVersionFile=.new_scripts.version
  echo "Check scripts version"
  download $1/download-desired-version/scripts?image=false ${newScriptsVersionFile}
  if [ ! -f ${scriptsVersionFile} ] || ! diff ${scriptsVersionFile} ${newScriptsVersionFile} >/dev/null; then
    version=`cat ${newScriptsVersionFile}`
    echo "Download scripts version ${version}"
    scriptsZipFile=.scripts.zip
    download $1/download-version/scripts/${version} ${scriptsZipFile}
    echo "Update scripts"
    scriptFiles="update_scripts.sh update.sh `basename "$0"` $2 $3 $4 $5"
    unzip -qo ${scriptsZipFile} $scriptFiles
    rm -f ${scriptsZipFile}
    chmod +x $scriptFiles
    mv ${newScriptsVersionFile} ${scriptsVersionFile}
    scriptsUpdated="true"
  else
    rm -f ${newScriptsVersionFile}
  fi
}