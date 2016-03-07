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
        DockerImage testImage = dockerClient.image().registry("frogops-docker-dockerv2-local.artifactoryonline.com").namespace("templates").repository("busybox").tag("latest")
        DockerImage deployImage = dockerClient.image().registry("artifactory.local:5001").namespace("templates").repository("busybox").tag("latest")
        this.dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry artifactory.local:5001")
                .addCommand(
                "docker pull " + testImage.getFullImageName() + "; " +
                        "docker tag " + testImage.getFullImageName() + " " + deployImage.getFullImageName() + "; " +
                        "curl  -uadmin:password https://artifactory.local:5001/v1/auth -k > ~/.dockercfg ; " +
                        "docker push " + deployImage.getFullImageName(), false)
        this.dockerContainer.doCreate()
        this.dockerContainer.startConfig.withPrivileges().addLink(this.artName, "artifactory.local").addLink(this.artName, "artifactory2.local").addLink(this.artName, "artifactory2.remote")

        when:
        def dockerLogs = this.dockerContainer.doStart(600).logs()

        then:
        dockerLogs.find(/job push\(${deployImage.getFullImageName(false)}\) = OK \(0\)/)

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
        dockerContainer.doCreate()
        dockerContainer.startConfig.withPrivileges().addLink(artName, "artifactory.local")

        when:
        def dockerLogs = dockerContainer.doStart(60).logs()

        then:
        dockerLogs.find("Login Succeeded")

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
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry "+subdomain+".art.local")
                .addCommand("docker login -u admin -p password -e auto-test@jfrog.com "+subdomain+".art.local", false)
        dockerContainer.doCreate()
        dockerContainer.startConfig.withPrivileges().addLink(artName, subdomain+".art.local")

        when:
        def dockerLogs = dockerContainer.doStart(60).logs()

        then:
        dockerLogs.find("Login Succeeded")

        cleanup:
        if (dockerContainer) {
            dockerContainer.doDelete(true, true)
        }

        where:
        subdomain            | _
        "docker-dev-local2"  | _
        "docker-prod-local2" | _
        "dockerv2"           | _
    }

    def setup() {
        dockerImage = getDockerClientForTesting()
        dockerContainer = dockerImage.getNewContainer()
        artName = artifactoryContainer.inspect().Name
    }

    private DockerImage getDockerClientForTesting() {
        dockerClient.image().registry("frogops-docker-dockerv2-local.artifactoryonline.com").repository("docker").tag("1.6.2").doCreate()
    }

    @Override
    String getDockerRepository() {
        return "artifactory-registry"
    }
}