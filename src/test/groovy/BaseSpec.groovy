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

import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.util.docker.DockerClient
import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage
import spock.lang.Shared
import spock.lang.Specification

import java.util.logging.Logger

/**
 * Created by matank on 22/03/15.
 */
abstract class BaseSpec extends Specification {

    @Shared
    DockerClient dockerClient = null
    @Shared
    ArtifactoryClient artifactoryClient = null
    @Shared
    Artifactory artifactoryAdmin = null
    @Shared
    DockerImage artifactoryImage = null
    @Shared
    DockerContainer artifactoryContainer = null
    @Shared
    String artifactoryVersion = null
    @Shared
    String bintrayRegistry = System.getProperty("bintray_registry")
    @Shared
    String dockerNamespace = System.getProperty("docker_namespace")
    @Shared
    protected static Logger logger = Logger.getLogger('baseSpecs')

    String dockerRepository = null

    def setupSpec() {
        def dockerUrl = System.getProperty('DOCKER_URL')
        dockerClient = new DockerClient(dockerUrl)
        artifactoryClient = new ArtifactoryClient()
        artifactoryVersion = System.getProperty("ARTIFACTORY_VERSION")
        artifactoryImage = dockerClient.image().registry(bintrayRegistry).namespace(dockerNamespace).repository(getDockerRepository()).tag("latest").doCreate()
        artifactoryContainer = artifactoryImage.getNewContainer().doCreate()
        artifactoryContainer.doStart()
        waitForArtifactory()
    }

    def "Test Artifactory expected version"() {
        def version
        when:
        version = artifactoryAdmin.system().version()
        then:
        version.version == artifactoryVersion
    }

    def cleanupSpec() {
        if (artifactoryContainer) {
            artifactoryContainer.doDelete(true, true)
            artifactoryContainer = null
        }
    }

    def waitForArtifactory() {
        artifactoryAdmin = artifactoryClient.create("http://${artifactoryContainer.inspect().NetworkSettings.IPAddress}:8081/artifactory/", "admin", "password")
        def retries = 40
        while (retries > 0) {
            if (artifactoryAdmin.system().ping()) {
                return
            }
            sleep 1000
            retries--

        }
    }

    abstract String getDockerRepository() ;
}
