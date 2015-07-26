#!/bin/bash
service nginx start
if [ -n "$RUNTIME_OPTS" ]; then
echo 'export JAVA_OPTIONS="$JAVA_OPTIONS '"$RUNTIME_OPTS"'"' >> /var/opt/jfrog/artifactory/etc/default
fi
. /etc/init.d/artifactory wait