#!/bin/bash
DIR="$( cd "$(dirname "$0")" ; pwd -P )"
echo DIR is $DIR
VERSION=0.12.0
export DISTDIR="$DIR/../pontus-dist/opt/pontus/pontus-knox/knox-$VERSION";



DEST_DIR=$DIR/../pontus-dist/opt/pontus/pontus-knox/current
CURDIR=`pwd`

if [[ ! -d $DEST_DIR ]] ; then
  printf "Must run the knox build first; please try again later \n"
  exit 0;
fi

CURDIR=`pwd`
cd $DIR
mvn -DskipTests clean install

cp $DIR/*/target/*.jar $DEST_DIR/lib

cd $DEST_DIR/lib
if [[ ! -f pontus-redaction-common-0.0.1-SNAPSHOT.jar ]]; then
  ln -s ../../../pontus-redaction/current/lib/pontus-redaction-common-0.0.1-SNAPSHOT.jar
fi 
cd $CURDIR
