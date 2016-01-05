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
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.util.docker.DockerContainer
import org.jfrog.util.docker.DockerImage

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

    def "Docker Login Test"() {
        setup:
        setRepositoryToForceAuthentication("docker-dev-local2")
        dockerContainer.createConfig
                .addEnv("DOCKER_DAEMON_ARGS", "--insecure-registry artifactory.local:5002")
                .addCommand("docker login -u admin -p password -e auto-test@jfrog.com artifactory.local:5002", false)
        dockerContainer.doCreate()
        dockerContainer.startConfig.withPrivileges().addLink(artName, "artifactory.local").addLink(artName, "artifactory2.local").addLink(artName, "artifactory2.remote")

        when:
        def dockerLogs = dockerContainer.doStart(60).logs()

        then:
        dockerLogs.find("Login Succeeded")

        cleanup:
        if (dockerContainer) {
            dockerContainer.doDelete(true, true)
        }

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

    def setRepositoryToForceAuthentication(String repoKey) {
        Map body = [
                "dockerApiVersion"         : "V2",
                "forceDockerAuthentication": true
        ]

        ArtifactoryRequest ar = new ArtifactoryRequestImpl()
                .apiUrl("api/repositories/$repoKey")
                .method(ArtifactoryRequest.Method.POST)
                .requestType(ArtifactoryRequest.ContentType.JSON)
                .responseType(ArtifactoryRequest.ContentType.TEXT)
                .requestBody(body)

        artifactoryAdmin.restCall(ar)
    }
}