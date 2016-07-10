FROM frogops-dockerv2.jfrog.io/os/centos-artifactory:6.6

MAINTAINER matank@jfrog.com

LABEL Install Nginx and create a valid certificate for the domain *.art.local
RUN yum install -y nginx && mkdir -p /etc/nginx/ssl && \
openssl req -nodes -x509 -newkey rsa:4096 -keyout /etc/nginx/ssl/demo.key -out /etc/nginx/ssl/demo.pem -days 356 \
-subj "/C=US/ST=California/L=SantaClara/O=IT/CN=*.art.local"
ADD nginx/artifactoryDocker.conf /etc/nginx/conf.d/default.conf

EXPOSE 8081 80 443 5000 5001 5002 5003 8001

ADD runArtifactoryWithNginx.sh /tmp/run.sh

RUN chmod +x /tmp/run.sh && yum install -y --disablerepo="*" --enablerepo="Artifactory-pro" jfrog-artifactory-pro

LABEL Create folders needed by Artifactory and set owner to artifactory user. \
Without this action, Artifactory will not be able to write to external mounts
RUN mkdir -p /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && \
chown -R artifactory: /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && \
mkdir -p /var/opt/jfrog/artifactory/defaults/etc && \
cp -rp /etc/opt/jfrog/artifactory/* /var/opt/jfrog/artifactory/defaults/etc/

ENV ARTIFACTORY_HOME /var/opt/jfrog/artifactory

LABEL Add default configuration containing Docker repositories
ADD artifactory/artifactory.config.xml /etc/opt/jfrog/artifactory/artifactory.config.xml

CMD /tmp/run.sh
