#!/bin/bash

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

function update_scripts {
  scripts_version_file=.scripts.version
  new_scripts_version_file=.new_scripts.version
  echo "Check scripts version"
  download $1/download-desired-version/scripts/?image=false ${new_scripts_version_file}
  if [ -f ${scripts_version_file} ] || ! diff ${scripts_version_file} ${new_scripts_version_file} >/dev/null; then
    version=`cat ${new_scripts_version_file}`
    echo "Download scripts version ${version}"
    scripts_zip_file=.scripts.zip
    download $1/download-version/scripts/${version} ${scripts_zip_file}
    echo "Update scripts"
    script_files="update_scripts.sh update.sh $2 $3 $4 $5"
    unzip -o ${scripts_zip_file} "$script_files" || exit 1
    rm -f ${scripts_zip_file}
    chmod +x "$script_files" || exit 1
    mv ${new_scripts_version_file} ${scripts_version_file} || exit 1
    echo "Restart $0"
    exec $0 "$@"
  fi

}