FROM centos:7
ADD ./jdk-11.0.12_linux-x64_bin.tar.gz /usr/local/oracle-java
RUN yum install -y epel-release
RUN yum -y update
RUN yum -y install which unzip zip wget
RUN curl -s "https://get.sdkman.io" | bash
ENV HOME "/root"
# RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install sbt

ENV SBT_VERSION=1.4.9
ENV JAVA_HOME=/usr/local/oracle-java/jdk-11.0.12
ENV SCALA_VERSION=2.11.12

RUN source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install scala 2.11.12 && sdk install sbt 1.4.9
ENV PATH "$PATH:/root/.sdkman/candidates/sbt/current/bin:/root/.sdkman/candidates/scala/current/bin:$JAVA_HOME:$JAVA_HOME/bin"

# ENTRYPOINT["/bin/bash"]
# RUN /bin/sh -c curl -fsL "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" | tar xfz - -C /usr/share &&   chown -R root:root /usr/share/sbt &&   chmod -R 755 /usr/share/sbt &&   ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt
# RUN /bin/sh -c sbt sbtVersion && mkdir -p project && echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt &&   echo "sbt.version=${SBT_VERSION}" > project/build.properties &&   echo "case object Temp" > Temp.scala &&   sbt compile &&   rm -r project && rm build.sbt && rm Temp.scala && rm -r target


