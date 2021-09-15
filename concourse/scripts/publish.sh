#!/bin/sh

set -eou

cd ./fauna-jvm-repository

PACKAGE_VERSION=$(sbt faunadb-common/version | tail -n 1 | cut -c 8-)

echo "Going to publish version: $PACKAGE_VERSION"

export SBT_OPTS="-XX:+UseParallelGC -XX:ReservedCodeCacheSize=1G -Xss200M -Xms256M -Xmx6G -Dsbt.log.noformat=true -Djava.net.preferIPv4Stack=true -Dhttp.connection.timeout=2 -Dhttp.connection-manager.timeout=2 -Dhttp.socket.timeout=6"

sbt faunadb-common/publishSigned
sbt faunadb-java/publishSigned
sbt +faunadb-scala/publishSigned
sbt sonatypeRelease
