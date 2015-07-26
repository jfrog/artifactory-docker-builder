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

import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage

class DockerRegistrySpec extends DockerProSpec {


    def "Push DOCKER image to local repositories"() {
        setup:
        DockerImage testImage = dockerClient.image().registry("frogops-docker-dockerv2-local.artifactoryonline.com").namespace("templates").repository("busybox").tag("latest")
        DockerImage deployImage = dockerClient.image().registry("artifactory.local:5001").namespace("templates").repository("busybox").tag("latest")
        DockerImage dockerImage = dockerClient.image().registry("frogops-docker-dockerv2-local.artifactoryonline.com").repository("docker").tag("1.6.2").doCreate()
        String artname = artifactoryContainer.inspect().Name
        DockerContainer dockerContainer = dockerImage.getNewContainer()
        dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry artifactory.local:5001")
                .addCommand(
                "docker pull " + testImage.getFullImageName() + "; " +
                        "docker tag " + testImage.getFullImageName() + " " + deployImage.getFullImageName() + "; " +
                        "curl  -uadmin:password https://artifactory.local:5001/v1/auth -k > ~/.dockercfg ; " +
                        "docker push " + deployImage.getFullImageName(), false)
        dockerContainer.doCreate()
        dockerContainer.startConfig.withPrivileges().addLink(artname, "artifactory.local").addLink(artname, "artifactory2.local").addLink(artname, "artifactory2.remote")

        when:
        def dockerLogs = dockerContainer.doStart(600).logs()

        then:
        dockerLogs.find(/job push\(${deployImage.getFullImageName(false)}\) = OK \(0\)/)

        cleanup:
        if (dockerContainer) {
            dockerContainer.doDelete(true, true)
        }
    }

    @Override
    String getDockerRepository() {
        return "artifactory-registry"
    }
}