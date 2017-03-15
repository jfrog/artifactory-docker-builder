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
import spock.lang.Unroll

class DockerRegistrySpec extends DockerProSpec {

    private DockerImage dockerImage
    private String artName
    private DockerContainer dockerContainer


    def "Push DOCKER image to local repositories"() {
        setup:
        DockerImage testImage = dockerClient.image().registry("frogops-dockerv2.jfrog.io").namespace("templates").repository("busybox").tag("latest")
        DockerImage deployImage = dockerClient.image().registry("artifactory.local:5001").namespace("templates").repository("busybox").tag("latest")
        this.dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry artifactory.local:5001")
                .addCommand(
                "docker pull ${testImage.getFullImageName()}; " +
                        "docker tag ${testImage.getFullImageName()} ${deployImage.getFullImageName()}; " +
                        "docker login -u admin -p password ${deployImage.getRegistry().registryHost} ; " +
                        "docker push ${deployImage.getFullImageName()}", false)

        this.dockerContainer.startConfig.withPrivileges().addLink(this.artName, "artifactory.local").addLink(this.artName, "artifactory2.local").addLink(this.artName, "artifactory2.remote")
        this.dockerContainer.doCreate()

        when:
        this.dockerContainer.doStart(600)

        then:
        this.dockerContainer.inspect().State.ExitCode.toInteger() == 0

        cleanup:
        if (this.dockerContainer) {
            this.dockerContainer.doDelete(true, true)
        }
    }

    @Unroll("Docker Login Test With Port #port")
    def "Docker Login Test With Ports"() {
        setup:
        dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry artifactory.local:" + port)
                .addCommand("docker login -u admin -p password -e auto-test@jfrog.com artifactory.local:" + port, false)
        dockerContainer.startConfig.withPrivileges().addLink(artName, "artifactory.local")
        dockerContainer.doCreate()

        when:
        dockerContainer.doStart(60)

        then:
        this.dockerContainer.inspect().State.ExitCode.toInteger() == 0

        cleanup:
        if (dockerContainer) {
            dockerContainer.doDelete(true, true)
        }

        where:
        port | _
        5001 | _
        5002 | _
        5003 | _
    }

    @Unroll("Docker Login Test With Sub-Domain #subdomain")
    def "Docker Login Test With Sub-Domains"() {
        setup:
        dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry " + subdomain + ".art.local")
                .addCommand("docker login -u admin -p password -e auto-test@jfrog.com " + subdomain + ".art.local", false)
        dockerContainer.startConfig.withPrivileges().addLink(artName, subdomain + ".art.local")
        dockerContainer.doCreate()

        when:
        dockerContainer.doStart(60)

        then:
        this.dockerContainer.inspect().State.ExitCode.toInteger() == 0

        cleanup:
        if (dockerContainer) {
            dockerContainer.doDelete(true, true)
        }

        where:
        subdomain            | _
        "docker-dev-local2"  | _
        "docker-prod-local2" | _
        "docker-virtual"     | _
    }

    def setup() {
        dockerImage = getDockerClientForTesting()
        dockerContainer = dockerImage.getNewContainer()
        dockerContainer.createConfig.setEntrypoint(["/usr/local/bin/wrapdocker"])
        artName = artifactoryContainer.inspect().Name
    }

    private DockerImage getDockerClientForTesting() {
        dockerClient.image().registry("frogops-dockerv2.jfrog.io").repository("docker").tag("1.11.2").doCreate()
    }

    @Override
    String getDockerRepository() {
        return "artifactory-registry"
    }
}