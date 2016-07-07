#Artifactory Docker image builder

##Examples

These examples provide Dockerfiles you can use to build the latest image of each Artifactory type.

##Build

###Artifactory-Centos
[View Dockerfile](artifactory-centos/Dockerfile)

`docker build -t frogops-dockerv2.jfrog.io/os/centos-artifactory:6.6 artifactory-centos`

*This is the base image for all Artifactory images*

###Artifactory-OSS

[View Dockerfile](artifactory-oss/Dockerfile)

`docker build -t artifactory-oss artifactory-oss`

###Artifactory-PRO

[View Dockerfile](artifactory-pro/Dockerfile)

`docker build -t artifactory-pro artifactory-pro`

###Artifactory-Registry

[View Dockerfile](artifactory-registry/Dockerfile)

`docker build -t artifactory-registry artifactory-registry`
