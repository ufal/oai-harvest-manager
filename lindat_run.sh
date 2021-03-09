#!/usr/bin/env bash
set -o errexit
set -o nounset
set -o pipefail
set -o errtrace
trap 's=$?; echo >&2 "$0: Error on line "$LINENO": $BASH_COMMAND"; exit $s' ERR
export SHELLOPTS

WD=$(dirname "$(readlink -e "$0")")
export WD
JAR=$(ls -t "$WD"/target/*.jar | head -n 1)

CONFIG="$1"
WORKDIR=$(xmllint --xpath '//workdir/text()' "$CONFIG")
export WORKDIR

LOG_DIR=${LOG_DIR:-"$WD/log"}

java -Dlogdir="${LOG_DIR}" -jar "${JAR}" "$@"

function process_result {
  if (( $# == 0 )); then
    echo >&2 "Error no params passed"
    exit 1;
  fi
  X=$(dirname "$1")
  mv "$1" "$X/dublin_core.xml"
  basename -s .xml "$1" > "$X/handle"
  ORIG_MD=$(find "$WD/$WORKDIR/results" -name "$(basename "$1")")
  cp "$ORIG_MD" "$X/"
  echo -e "$(basename "$ORIG_MD")\tbundle:ORIGINAL\tdescription:original metadata" > "$X/contents"
}

export -f process_result

find "$WD/$WORKDIR/results/dspace" -name '*.xml' -exec bash -c 'process_result $1' _ {} \;

mv "$WD/$WORKDIR/results/dspace" "$WD/import"



