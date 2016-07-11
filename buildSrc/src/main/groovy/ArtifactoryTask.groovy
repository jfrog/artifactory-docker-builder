/*
 * Copyright (C) 2015 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskValidationException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage
import org.jfrog.util.docker.DockerRegistry
import org.jfrog.util.docker.configurations.DockerFileBuilder

/**
 * Created by matank on 7/8/15.
 */
class ArtifactoryTask extends BaseTask {

    DockerImage artifactoryImage
    boolean pushToArtifactory = false
    boolean enableNginx = false
    boolean createLatestTag = false
    String dockerNamespace = null
    String tag = null

    ArtifactoryTask() {
        description = "Build artifactory docker image"
    }

    @TaskAction
    void build() {
        initArtifactoryImage()
        createDockerFile()

        buildArtifactoryImage()

        pushArtifactoryImage()

    }

    @Override
    void createDockerFile() {
        dfb = new DockerFileBuilder(project.projectDir.absolutePath + "/tmp")

        dfb.from centosImage.getFullImageName()
        dfb.maintainer "matank@jfrog.com"
        dfb.label("Install Nginx and create certificate valid for the domain *.art.local")
        if (enableNginx) {
            dfb.run 'yum install -y nginx && \
mkdir -p /etc/nginx/ssl && \\\n\
openssl req -nodes -x509 -newkey rsa:4096 -keyout /etc/nginx/ssl/demo.key -out /etc/nginx/ssl/demo.pem -days 356 \\\n\
-subj "/C=US/ST=California/L=SantaClara/O=IT/CN=*.art.local"'
            dfb.add this.getClass().getResource("nginx/artifactoryDocker.conf").path, "/etc/nginx/conf.d/default.conf"
        }
        exposePorts(dfb)
        if (enableNginx) {
            dfb.add this.getClass().getResource("artifactory/runArtifactoryWithNginx.sh").path, "/tmp/run.sh"
        } else {
            dfb.add this.getClass().getResource("artifactory/runArtifactory.sh").path, "/tmp/run.sh"
        }
        def artifactoryPackage = null
        if (artifactoryVersion != "latest" && Integer.parseInt(artifactoryVersion[0]) < 4) {
            artifactoryPackage = "artifactory"
        } else {
            artifactoryPackage = "jfrog-artifactory-" + artifactoryType
        }
        if (artifactoryVersion == "latest" || Integer.parseInt(artifactoryVersion[0]) >= 4) {
            dfb.run 'chmod +x /tmp/run.sh && yum install -y --disablerepo="*" --enablerepo="Artifactory-' + artifactoryType +
                    '" ' + artifactoryPackage + getVersionToInstall()
        } else {
            if (artifactoryType == "oss") {
                dfb.run 'chmod +x /tmp/runArtifactory.sh && \
yum install -y --disablerepo="*" http://frogops.jfrog.io/frogops/artifactory-' + artifactoryType + '/artifactory-' + artifactoryVersion + '.rpm'
            } else {
                dfb.run 'chmod +x /tmp/runArtifactory.sh && \
yum install -y --disablerepo="*" http://frogops.jfrog.io/frogops/artifactory-' + artifactoryType + '/org/artifactory/powerpack/artifactory-powerpack-rpm/' +
                        artifactoryVersion + '/artifactory-powerpack-rpm-' + artifactoryVersion + '.rpm'
            }
        }
        dfb.label("Create Folders that aren't exists, and make sure they are owned by Artifactory, \\\n\
Without this action, Artifactory will not be able to write to external mounts")
        dfb.run 'mkdir -p /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && \\\n\
chown -R artifactory: /etc/opt/jfrog/artifactory /var/opt/jfrog/artifactory/{data,logs,backup} && mkdir -p /var/opt/jfrog/artifactory/defaults/etc && \\\n\
cp -rp /etc/opt/jfrog/artifactory/* /var/opt/jfrog/artifactory/defaults/etc/'
        dfb.env 'ARTIFACTORY_HOME', "/var/opt/jfrog/artifactory"
        dfb.cmd "/tmp/run.sh"
        if (enableNginx) {
            dfb.add this.getClass().getResource("artifactory/artifactory.config.xml").path, '/etc/opt/jfrog/artifactory/artifactory.config.xml'
        }

        dfb.create()

    }

    private DockerFileBuilder exposePorts(DockerFileBuilder dfb) {
        List portsToExpose = [8081]
        if (enableNginx) {
            portsToExpose.addAll([80, 443, 5000, 5001, 5002, 5003, 8001])
        }
        dfb.expose(portsToExpose as int[])
    }

    private void initArtifactoryImage() {
        this.artifactoryImage = dockerClient.image()
                .registry(registry)
                .namespace(dockerNamespace)
                .repository("artifactory-" + (enableNginx ? "registry" : artifactoryType))
                .tag(getTagWithBuildNumber())
    }

    private void buildArtifactoryImage() {
        dockerClient.build(dfb, artifactoryImage)
        println "################### BEGIN DOCKERFILE ######################"
        println dfb.getDockerfile().text
        println "#################### END DOCKERFILE #######################"
        dfb.close() //Close DockerFileBuilder to remove any leftovers files from the build process
    }

    private def testArtifactoryImage() {

        println "TEST: Start testing ${artifactoryImage.getFullImageName()}"

        URL artifactoryLicense = this.getClass().getResource("artifactory/artifactory.lic")

        if (artifactoryType != "oss" && !artifactoryLicense) {
            logger.warn("WARNING: Artifactory license is not present, Skipping test phase")
            return false
        }

        int portBound = 8888
        DockerContainer artifactoryContainer = artifactoryImage.getNewContainer("artifactory-" + artifactoryType)
        artifactoryContainer.startConfig.addPortBinding(8081, "tcp", "0.0.0.0", portBound)

        try {
            println "TEST: Creates artifactory container bound to port $portBound"
            artifactoryContainer.doCreate()
        } catch (Exception e) {
            logger.warn("WARNING: ${artifactoryContainer.name} container was already configured, removing and recreating it")
            artifactoryContainer.doDelete(true, true)
            artifactoryContainer.doCreate()
        }

        try {
            if (artifactoryType != "oss") {
                artifactoryContainer.startConfig.addBinds(artifactoryLicense.path, ARTIFACTORY_HOME + "/etc/artifactory.lic")
                println "TEST: using license located at ${ARTIFACTORY_HOME}etc/artifactory.lic"
            }

            println "TEST: Starts artifactory container bound to port $portBound"
            artifactoryContainer.doStart()
            waitUntilArtifactoryIsUp()
            println "TEST: Stop artifactory container"
            artifactoryContainer.doStop()
        } finally {
            println "TEST: Delete artifactory container"
            artifactoryContainer.doDelete(true, true)
        }
        return true
    }

    private void waitUntilArtifactoryIsUp() {
        println "TEST: Wait until artifactory is fully up"
        Artifactory artifactory = ArtifactoryClient.create("http://localhost:8888/artifactory", "admin", "password")
        int maxTimeToWaitInSec = 60
        while (maxTimeToWaitInSec > 0) {
            if (artifactory.system().ping()) {
                return
            }
            sleep 1000
            maxTimeToWaitInSec--
        }
        throw new TaskValidationException("Artifactory " + artifactoryType + " is not up")
    }

    private void pushArtifactoryImage() {
        if (!testArtifactoryImage()) {
            return
        }
        if (pushToArtifactory) {
            artifactoryImage.registry(new DockerRegistry(
                    registry,
                    registryUser,
                    registryPassword))
            artifactoryImage.doPush()

            if (createLatestTag) {
                DockerImage latestImage = dockerClient.image()
                        .registry(artifactoryImage.getRegistry())
                        .namespace(artifactoryImage.getNamespace())
                        .repository(artifactoryImage.getRepository())
                        .tag("latest")
                artifactoryImage.doTag(latestImage, true)
                latestImage.doPush()
            }
        } else {
            logger.info("Push image is disabled")
        }
    }

    void setPushToArtifactory(boolean pushToArtifactory) {
        this.pushToArtifactory = pushToArtifactory
    }

    void setArtifactoryRegistryUsername(String artifactoryRegistryUsername) {
        this.artifactoryRegistryUsername = artifactoryRegistryUsername
    }

    void setArtifactoryRegistryPassword(String artifactoryRegistryPassword) {
        this.artifactoryRegistryPassword = artifactoryRegistryPassword
    }

    void setEnableNginx(boolean enableNginx) {
        this.enableNginx = enableNginx
    }

    void setCreateLatestTag(boolean createLatestTag) {
        this.createLatestTag = createLatestTag
    }

    void setDockerNamespace(String dockerNamespace) {
        this.dockerNamespace = dockerNamespace
    }

    void setTag(String tag) {
        this.tag = tag
    }

    String getVersionToInstall() {
        if (artifactoryVersion == "latest") {
            return ""
        }

        return "-" + artifactoryVersion + (StringUtils.isBlank(artifactoryBuildNumber) ? "-" + artifactoryBuildNumber : "")
    }

    String getTagWithBuildNumber() {
        return tag + (StringUtils.isBlank(artifactoryBuildNumber) ? "-" + artifactoryBuildNumber : "")
    }
}
