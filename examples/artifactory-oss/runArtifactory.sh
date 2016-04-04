#!/bin/bash
ARTIFACTORY_FOLDERS=$(mount | grep artifactory | awk '{print $3}')

function checkFolderPermissions {
    DIR=$1

    STAT=( $(stat -Lc "%A %G %U" $1) )
    PERM=${STAT[0]}
    GROUP=${STAT[1]}
    USER=${STAT[2]}

    if [[ $USER != "artifactory" ]] || [[ $GROUP != "artifactory"  ]] ; then
            if [[ ! $PERM =~ d......rw.  ]] ; then
                    echo "[WARN] artifactory user doesn't have read & write permissions on $1, adding permissions"
                    chmod a+wr $1
            fi
    elif [[ $USER == "artifactory" ]] && [[ ! $PERM =~ drw....... ]] ; then
            echo "[WARN] artifactory user doesn't have read & write permissions on $1, adding permissions"
            chmod u+wr $1
    elif [[ $GROUP == "artifactory" ]] && [[ ! $PERM =~ d...rw.... ]] ; then
            echo "[WARN] artifactory user doesn't have read & write permissions on $1, adding permissions"
            chmod g+wr $1
    fi
}

function restoreEtcFilesIfEmpty {
    #If etc folder doesn't exists OR exists but default file is missing, copies the default content from $ARTIFACTORY_HOME/defaults
    ETC_FOLDER=$ARTIFACTORY_HOME/etc
    if [[ ! -d $ETC_FOLDER ]] || [[ ! -e $ETC_FOLDER/default ]] ; then
        echo "[WARN] $ETC_FOLDER is either empty or missing default files, creating default files"
        cp -rpn $ARTIFACTORY_HOME/defaults/etc/* $ETC_FOLDER/
    fi
}

for folder in $ARTIFACTORY_FOLDERS ; do
    checkFolderPermissions $folder
done

restoreEtcFilesIfEmpty

if [ -n "$RUNTIME_OPTS" ]; then
    echo 'export JAVA_OPTIONS="$JAVA_OPTIONS '"$RUNTIME_OPTS"'"' >> /etc/opt/jfrog/artifactory/default
fi
if [ -n "$(grep artdist /var/opt/jfrog/artifactory/etc/default)" ] ; then
    sed -i s/-Dartdist=rpm/-Dartdist=docker/g /etc/opt/jfrog/artifactory/default
else
    echo export JAVA_OPTIONS='"$JAVA_OPTIONS -Dartdist=docker"' >> /etc/opt/jfrog/artifactory/default
fi
. /etc/init.d/artifactory wait