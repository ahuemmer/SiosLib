#!/bin/bash

versionParameter=$1
echo "version = $versionParameter"

# Get version number from version tag

if [[ ${versionParameter} == v* ]]; then
  JAR_VERSION=`echo $1 | cut -d'v' -f2`
else
  JAR_VERSION=${versionParameter}
fi
echo "jar = $JAR_VERSION"

./gradlew -si -Pversion=${JAR_VERSION} build sourcesJar javadocJar publish
