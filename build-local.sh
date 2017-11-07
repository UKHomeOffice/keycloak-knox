#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
VERSION=1.0.0-SNAPSHOT
echo DIR is $DIR
export DISTDIR="$DIR/../pontus-dist/opt/pontus/pontus-keycloak/pv-gdpr-$VERSION";

CURDIR=`pwd`
cd $DIR
mvn -DskipTests clean install

if [[ ! -d $DISTDIR ]]; then
  mkdir -p $DISTDIR
fi

cd $DISTDIR

rm -rf *


cp -r $DIR/bin $DIR/conf $DISTDIR
mkdir -p $DISTDIR/lib

cp $DIR/target/*.jar $DISTDIR/lib

cd ..

ln -s pv-gdpr-$VERSION current

cd $CURDIR
