#!/bin/sh

set -eou

cd ./fauna-jvm-repository

export SBT_OPTS="-Dsbt.log.noformat=true -Djava.net.preferIPv4Stack=true -Dhttp.connection.timeout=2 -Dhttp.connection-manager.timeout=2 -Dhttp.socket.timeout=6 -XX:+UseParallelGC -XX:ReservedCodeCacheSize=1G -Xss200M -Xms256M -Xmx6G"

PACKAGE_VERSION=$(sbt faunadb-common/version | tail -n 1 | cut -c 8-)

echo "Going to publish version: $PACKAGE_VERSION"

echo "$GPG_PRIVATE_KEY" > gpg_private_key
echo "$GPG_PUBLIC_KEY" > gpg_public_key

export GPG_PRIVATE_KEY=gpg_private_key
export GPG_PUBLIC_KEY=gpg_public_key

sbt faunadb-common/publishSigned
sbt faunadb-java/publishSigned
sbt +faunadb-scala/publishSigned
sbt sonatypeRelease

echo "faunadb-jvm@$PACKAGE_VERSION has been published (but sometimes it can take up to 2 days before it appears in maven repository)" > ../slack-message/publish
