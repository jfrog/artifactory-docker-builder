#Artifactory Docker image builder
###This project is a nice template to build & push artifactory docker images to artifactory docker registry and publish to bintray for distribution:

###General:
This project is gradle-groovy based using [docker-remote-util](https://github.com/JFrogDev/docker-remote-util) library to interact with docker remote API.

##Public Usage:
./gradlew createArtifactoryOss -Partifactory_version=3.9.2 - Builds artifactory OSS 3.9.2 on a centos-6.6 with openjdk-1.8.0
./gradlew createArtifactoryRegistry - Builds artifactory pro latest version ready as docker registry

###Gradle Tasks:
1. createCentos - Create base centos image for artifactory
2. createArtifactoryOss - Create clean artifactory oss image
3. createArtifactoryPro - Create clean artifactory pro image
4. createArtifactoryRegistry - Create artifactory image with nginx and docker registry configured
5. publishImagesToBintray - Push tag/latest from Artifactory  to Bintray "https://bintray.com/jfrog/registry"
6. test - Download the latest tag from bintray and run sanity test on the image
7. release - Running all tasks

###Project Properties:
1. artifactory_version - the requested artifactory version to build, Default: latest available release version
2. artifactory_contextUrl - artifactory URL, [http|s]://[hostname]:[port]/artifactory
3. artifactory_user, artifactory_password = When publishing to Artifactory registry provide Credentials with delete privileges to override latest tag
4. registry - Docker registry to host built images
5. docker_repo - Artifactory local repository hosting your images
6. bintray_subject - Bintray User/Organiziation
7. bintray_registry - Bintray Docker registry

###Prerequisities
It is always good practice to test and verify your images before distributing. <br>
This project runs a sanity test after the image has been built, which is a precondition to push to artifactory. <br>
For the pro version artifactory license must be places under 'artifactory-docker-builder/buildSrc/src/main/resources/artifactory/artifactory.lic'. <br>
As final step after the image has been published to bintray, the latest tags are being downloaded and verified accordingly, the pro version requires the license to be placed under 'artifactory-docker-builder/src/test/resources/artifactory.lic'
