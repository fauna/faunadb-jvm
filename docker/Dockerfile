FROM centos:7
# FROM karlkfi/concourse-dcind:latest

ENV HOME="/root" \
#     DOCKER_CHANNEL=stable \
#     DOCKER_VERSION=19.03.2 \
#     DOCKER_COMPOSE_VERSION=1.24.1 \
#     DOCKER_SQUASH=0.2.0 \
    SBT_VERSION=1.4.9 \
    JAVA_HOME=/usr/local/oracle-java/jdk-11.0.12 \
    SCALA_VERSION=2.11.12

ADD ./jdk-11.0.12_linux-x64_bin.tar.gz /usr/local/oracle-java
# ADD ./entrypoint.sh /usr/local
# RUN chmod 755 /usr/local/entrypoint.sh && \
#     yum install -y epel-release && \
#     yum -y update && \
#     yum -y install which \
#     unzip zip wget && \
#     curl -s "https://get.sdkman.io" | bash && \
#     yum install -y yum-utils && \
#     yum-config-manager \
#         --add-repo \
#         https://download.docker.com/linux/centos/docker-ce.repo && \
#     yum install -y docker-ce-${DOCKER_VERSION} docker-ce-cli-${DOCKER_VERSION} containerd.io && \
#     curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose && \
#     chmod +x /usr/local/bin/docker*
#
#
RUN yum install -y epel-release && \
    yum -y update && \
    yum -y install which unzip zip wget && \
    curl -s "https://get.sdkman.io" | bash && \
    source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install scala 2.11.12 && \
    sdk install sbt 1.4.9

ENV PATH "$PATH:/root/.sdkman/candidates/sbt/current/bin:/root/.sdkman/candidates/scala/current/bin:$JAVA_HOME:$JAVA_HOME/bin"

# RUN /bin/sh -c curl -fsL "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" | tar xfz - -C /usr/share &&   chown -R root:root /usr/share/sbt &&   chmod -R 755 /usr/share/sbt &&   ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt
# RUN /bin/sh -c sbt sbtVersion && mkdir -p project && echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt &&   echo "sbt.version=${SBT_VERSION}" > project/build.properties &&   echo "case object Temp" > Temp.scala &&   sbt compile &&   rm -r project && rm build.sbt && rm Temp.scala && rm -r target

# ENTRYPOINT ["entrypoint.sh", "bash", "-ceu"]