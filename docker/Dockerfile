FROM centos:7

ENV HOME="/root" \
    SBT_VERSION=1.4.9 \
    JAVA_HOME=/usr/local/oracle-java/jdk-11.0.12 \
    SCALA_VERSION=2.11.12

ADD ./jdk-11.0.12_linux-x64_bin.tar.gz /usr/local/oracle-java
RUN yum install -y epel-release && \
    yum -y update && \
    yum -y install which unzip zip wget && \
    curl -s "https://get.sdkman.io" | bash && \
    source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install sbt 1.4.9

ENV PATH "$PATH:/root/.sdkman/candidates/sbt/current/bin:/root/.sdkman/candidates/scala/current/bin:$JAVA_HOME:$JAVA_HOME/bin"
