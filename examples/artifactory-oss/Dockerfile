FROM frogops-dockerv2.jfrog.io/os/centos-artifactory:6.6

MAINTAINER matank@jfrog.com

EXPOSE 8081

ADD runArtifactory.sh /tmp/run.sh
RUN chmod +x /tmp/run.sh && \
    yum install -y --disablerepo="*" --enablerepo="Artifactory-oss" jfrog-artifactory-oss

LABEL Create folders needed by Artifactory and set owner to artifactory user. \
Without this action, Artifactory will not be able to write to external mounts
RUN mkdir -p /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && \
    chown -R artifactory: /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && \
    mkdir -p /var/opt/jfrog/artifactory/defaults/etc && \
    cp -rp /etc/opt/jfrog/artifactory/* /var/opt/jfrog/artifactory/defaults/etc/

ENV ARTIFACTORY_HOME /var/opt/jfrog/artifactory

CMD /tmp/run.sh
