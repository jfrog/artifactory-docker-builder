FROM frogops-dockerv2.jfrog.io/centos:6.6
LABEL This is the official Centos image

MAINTAINER matank@jfrog.com

LABEL Add Artifactory Yum repositories
ADD ["Artifactory-centos.repo","Artifactory-oss.repo","Artifactory-pro.repo","nginx.repo","/etc/yum.repos.d/"]

RUN rm -rf /etc/yum.repos.d/Centos* && \
yum install java-1.8.0-openjdk-devel rsync net-tools -y && \
yum clean all
