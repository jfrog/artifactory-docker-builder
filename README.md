#Artifactory Docker image builder
###This project serves as a template you can use to build Artifactory Docker images, push them to an Artifactory Docker registry, and from them, publish them to bintray for distribution:

###General:
The project is Gradle-Groovy based using JFrog's [docker-remote-util](https://github.com/JFrogDev/docker-remote-util) library to interact with the Docker Remote API.

##Public Usage:
./gradlew createArtifactoryOss -Partifactory_version=3.9.2 - Builds Artifactory OSS 3.9.2 on a centos-6.6 with openjdk-1.8.0 <br>
./gradlew createArtifactoryRegistry - Builds the latest version of Artifactory Pro preconfigured as a Docker registry

###Gradle Tasks:
1. createCentos - Creates the base Centos image for Artifactory
2. createArtifactoryOss - Creates a clean Artifactory OSS image
3. createArtifactoryPro - Creates a clean Artifactory Pro image
4. createArtifactoryRegistry - Creates an Artifactory image with NGINX and pre-configured with a Docker registry 
5. publishImagesToBintray - Pushes tag/latest from Artifactory  to Bintray repository "https://bintray.com/jfrog/reg2"
6. test - Downloads the latest tag from bintray and runs a sanity test on the image
7. createAll - Builds each type of Artifactory Docker image (Oss, Pro, Docker-registry)
8. release - Runs all tasks

###Project Properties:
1. artifactory_version - the requested artifactory version to build, Default: latest available release version
2. artifactory_contextUrl - artifactory URL, [http|s]://[hostname]:[port]/artifactory
3. artifactory_user, artifactory_password = When publishing to Artifactory registry provide Credentials with delete privileges to override latest tag
4. registry - Docker registry to host built images
5. docker_repo - Artifactory local repository hosting your images
6. bintray_subject - Bintray User/Organiziation
7. bintray_registry - Bintray Docker registry

###Prerequisities
It is always good practice to test and verify your images before distributing them. <br>
Before pushing an image to Artifactory, this project runs a sanity test on it. <br>
For the Pro version, the Artifactory license must be placed under 'artifactory-docker-builder/buildSrc/src/main/resources/artifactory/artifactory.lic'. <br>
As a final step after the image has been published to Bintray, the latest tags are downloaded and verified accordingly. The pro version requires the license to be placed under 'artifactory-docker-builder/src/test/resources/artifactory.lic'

###Examples
Examples of the Dockerfiles for each Artifactory image (Oss, Pro and Registry) can be found in [examples](examples) 
