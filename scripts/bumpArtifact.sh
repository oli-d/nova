#!/bin/bash

# This script can be used to bump the version of a mvn artifact. It expects the following input:
#
#  $1: ID of the artifact of which the version should be bumped
#  $2: new version number
#  $3: prefix for each line that is printed out (optional)
#
# The script invokes an appropriate mvn versions:set command and checks whether any dependant artifacts in a 
# multi module project were affected by the version bump. If it detects such artifacts, it calls itself 
# recursively to also bump their versions


# Bash v3 does not support associative arrays
# Usage: map_put map_name key value
function map_put
{
    alias "${1}$2"="$3"
}

# map_get map_name key
function map_get
{
    alias "${1}$2" | awk -F"'" '{ print $2; }'
}

# map_keys map_name
function map_keys
{
    alias -p | grep $1 | cut -d'=' -f1 | awk -F"$1" '{print $2; }'
}

function determineReactorArtifactId() {
  REACTOR_ARTIFACT_ID=`mvn --non-recursive -q -Dexec.executable="echo" -Dexec.args='${project.artifactId}' org.codehaus.mojo:exec-maven-plugin:1.3.1:exec`
  echo "  reactor artifactId = $REACTOR_ARTIFACT_ID"
}

function determineAllArtifactVersions() {
  if [ "`map_keys versions`" == "" ]; then
    echo "  Getting all version numbers..."
    ARTIFACT_VERSIONS=`mvn -o -q -Dexec.executable="echo" -Dexec.args='${project.artifactId}=${project.version}' org.codehaus.mojo:exec-maven-plugin:1.3.1:exec`
    for DEF in $ARTIFACT_VERSIONS; do
        A=`echo ${DEF} | cut -d '=' -f 1`
        V=`echo ${DEF} | cut -d '=' -f 2`
        map_put versions $A $V
        # echo "  $A: $V"
    done
  fi
}


function getArtifactVersion() {
  echo `map_get versions $1`
}

function bumpVersion() {
    if [ "$2" != "" ]; then
        mvn -o versions:set -DgenerateBackupPoms=false -oldVersion=* -DgroupId=* -DartifactId=$1 -DnewVersion=$2
    fi
}

function getArtifactFromMavenVersionsOutput() {
    # the items should be of form <groupId>:<artifact>
    echo $1 | cut -d ':' -f 2
}

function updateArtifact() {
    echo "$3($1) Updating version of artifact \"$1\" to \"$2\"..."
    OUTPUT=`bumpVersion $1 $2 | grep -v "Processing change of" | grep -i processing`

    # TODO: exit if mvn command was unsuccessful
    echo "$3($1) Update successful :-)"
    map_put versions $1 $2

    for ITEM in ${OUTPUT}; do
      # find all modified artifacts
      if [[ "$ITEM" == ch.squaredesk* ]]; then
        ARTIFACT=`getArtifactFromMavenVersionsOutput ${ITEM}`
        if [ "$ARTIFACT" != "$1" ] && [ "$ARTIFACT" != "$REACTOR_ARTIFACT_ID" ]; then
          CURRENT_VERSION=`getArtifactVersion ${ARTIFACT}`
          # TODO: check if artifact was already changed
          echo "$3($1) Artifact $ARTIFACT:$CURRENT_VERSION depends on $1. If you also want to bump its version,"
          read -p "$3($1)   specify the desired new version (leave blank to skip) > " NEW_VERSION
          # recursively invoke script
          updateArtifact ${ARTIFACT} ${NEW_VERSION} "$3    "
        fi
      fi
    done
}

# TODO: check parameters

echo ""
echo "Installing all dependencies locally..."
mvn install -Dmaven.test.skip=true

echo ""
echo "Gathering information..."
determineAllArtifactVersions
determineReactorArtifactId

echo ""
updateArtifact $1 $2 $3

echo ""
REACTOR_VERSION=`getArtifactVersion ${REACTOR_ARTIFACT_ID}`
echo "After those changes, reactor $REACTOR_ARTIFACT_ID:$CURRENT_VERSION should also be updated."
read -p "Please specify the desired new version (leave blank to skip) > " NEW_VERSION
if [ "$NEW_VERSION" != "" ]; then
    bumpVersion ${REACTOR_ARTIFACT_ID} ${NEW_VERSION}
fi


