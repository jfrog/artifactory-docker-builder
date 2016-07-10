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

import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction
import org.jfrog.util.docker.DockerClient
import org.jfrog.util.docker.DockerImage
import org.jfrog.util.docker.configurations.DockerFileBuilder

/**
 * Created by matank on 7/8/15.
 */
abstract class BaseTask extends DefaultTask {

    protected static Logger logger = Logging.getLogger(BaseTask)

    protected final String ARTIFACTORY_HOME = '/var/opt/jfrog/artifactory/'

    String hostUrl
    DockerClient dockerClient
    String registry
    String registryUser
    String registryPassword
    DockerFileBuilder dfb
    DockerImage centosBaseImage
    DockerImage centosImage

    //######### ARTIFACTORY OBJECTS #############//
    String artifactoryVersion

    String artifactoryType

    abstract void createDockerFile();

    @TaskAction
    void Build() {
        this.dockerClient = new DockerClient(hostUrl)
        this.centosBaseImage = initCentosBaseImage()
        this.centosImage = initCentosImage()
    }

    /**
     * Init Centos Base Image.
     */
    DockerImage initCentosBaseImage() {
        dockerClient.image()
                .registry(registry)
                .repository("centos")
                .tag("6.6")
    }

    DockerImage initCentosImage() {
        dockerClient.image()
                .registry(registry)
                .namespace("os")
                .repository("centos-artifactory")
                .tag("6.6")
    }

    void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl
        logger.info("Using $hostUrl docker server.")
    }

    void setRegistry(String registry) {
        this.registry = registry
        logger.info("Using $registry docker registry.")
    }

    void setRegistryUser(String registryUser) {
        this.registryUser = registryUser
    }

    void setRegistryPassword(String registryPassword) {
        this.registryPassword = registryPassword
    }

    void setArtifactoryVersion(String artifactoryVersion) {
        this.artifactoryVersion = artifactoryVersion
    }

    void setArtifactoryType(String artifactoryType) {
        this.artifactoryType = artifactoryType
    }
}
