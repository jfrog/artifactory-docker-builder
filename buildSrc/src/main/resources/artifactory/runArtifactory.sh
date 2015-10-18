#!/bin/bash
service nginx start
if [ -n "$RUNTIME_OPTS" ]; then
echo 'export JAVA_OPTIONS="$JAVA_OPTIONS '"$RUNTIME_OPTS"'"' >> /var/opt/jfrog/artifactory/etc/default
fi
if [ -n "$(grep artdist /var/opt/jfrog/artifactory/etc/default)" ] ; then
    sed -i s/-Dartdist=rpm/-Dartdist=docker/g /etc/opt/jfrog/artifactory/default
else
    echo export JAVA_OPTIONS='"$JAVA_OPTIONS -Dartdist=docker"' >> /var/opt/jfrog/artifactory/etc/default
fi
. /etc/init.d/artifactory wait