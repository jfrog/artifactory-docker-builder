package docker

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import org.apache.commons.lang.StringUtils

/**
 * Created by matank on 12/22/2014.
 */
class DockerContainer {

    DockerServer dockerServer

    String id
    String ip

    boolean isPriveleged = false

    String image
    def commands = []

    /**
     * Init DockerContainer, to init, Map is required.
     * dockerServer = [DockerServer object]
     * id = containerId
     *
     * @param args
     */
    DockerContainer(Map<String, ?> args) {
        this.dockerServer = args.remove("dockerServer")
        this.id = args.remove("id")
//        this.ip = getContainerInfo().NetworkSettings.IPAddress
    }

    DockerContainer(DockerServer dockerServer, String image) {
        this.dockerServer = dockerServer
        this.image = image
    }

    /**
     * Collect logs of the container.
     *
     * @param containerId
     * @return ByteArrayInputStream - ".text" can be used to view as text
     */
    def getLogs() {
        def logs = null
        int counter = 0
        while (logs == null && counter <= 3) {
            def response = DockerServer.restClient.get(
                    path: "/containers/$id/logs",
                    query: [stdout: 1, stderr: 1],
                    contentType: ContentType.BINARY
            )
            logs = response.getData()
            counter++
            sleep(2000)
        }

        return logs
    }

    /**
     * Get file from a container,
     *
     * @param fileToDownload - the file from the container to download
     * @param destinationFile - can be string or a file
     * @return - File from the container as file
     */
    def getFile(fileToDownload, destinationFile) {
        HttpResponseDecorator response = dockerServer.restClient.post(
                path: "/containers/$id/copy",
                body: [Resource: fileToDownload],
                requestContentType: ContentType.JSON,
                contentType: ContentType.BINARY
        )

        if (destinationFile) {
            File file = (destinationFile instanceof File ? destinationFile : new File(destinationFile))

            if (file.exists()) {
                file.delete()
            }

            if (!file.exists()) {
                file.getParentFile().mkdirs()
                file.createNewFile()
            }

            file.append(response.getData())
            while (file.size() < response.getData().available()) {
                sleep(500)
            }
            return file
        }

        return response.getData().text
    }


    DockerContainer create() {

        def config = containerConfiguration()

        try {
            def result = dockerServer.restClient.post(
                    path: "/containers/create",
                    body: config.toString(),
                    requestContentType: ContentType.JSON
            )

            this.id = result.getData().Id
        } catch (HttpResponseException hre) {
            println "Response Message: ${hre.getMessage()}"
            println "Response Body: ${hre.response.getData().text}"
            throw hre
        }

        return this
    }

    DockerContainer execCreate(String cmd) {
        HttpResponseDecorator result = dockerServer.restClient.post(
                path: "/containers/$id/exec",
                contentType: ContentType.JSON,
                body: [
                        "AttachStdin" : false,
                        "AttachStdout": true,
                        "AttachStderr": true,
                        "Tty"         : false,
                        "Cmd"         : [
                                "/bin/bash",
                                "-c",
                                cmd
                        ]
                ]
        )

        return new DockerContainer([dockerServer: dockerServer, id: result.getData().Id])
    }

    String getContainerName() {
        try {

            def result = dockerServer.restClient.get(
                    path: "/containers/$id/json",
                    requestContentType: ContentType.JSON
            )
            String name = result.getData().Name
            return StringUtils.strip(name, '/')
        } catch (HttpResponseException ex) {
            throw new RuntimeException("Could not find container ID: $id", ex)
        }


    }

    String exec() {
        def result = dockerServer.restClient.post(
                path: "/exec/$id/start",
                requestContentType: ContentType.JSON,
                body: [
                        "Detach": false,
                        "Tty"   : false
                ]
        )

        return result.getData().text
    }


    String startContainer(boolean waitForContainerToStopRequired, List links, int sleepBetweenRequestsInSec = 1, boolean serverPortBindings = false) {
        def bindings = dockerServer.portBindings
        def bodyContent = ["Links": links, "Privileged": isPriveleged]
        if (serverPortBindings) {
            bodyContent.put("PortBindings", bindings)
        }
        def result = dockerServer.restClient.post(
                path: "/containers/$id/start",
                body: bodyContent,
                requestContentType: ContentType.JSON
        )

        if (waitForContainerToStopRequired) {
            waitForContainerToStop(sleepBetweenRequestsInSec * 1000)
        }

        return getLogs().text
    }



    def removeContainer() {
        try {
             dockerServer.restClient.delete(
                    path: "/containers/$id",
                    query: [force: 1]
            )
        } catch (HttpResponseException hre) {
            println "Container $id couldn't be removed with the following error ${hre.getResponse().getStatus()}"
            println "${hre.getMessage()}"
        }
    }

    def waitForContainerToStop(int waitBetweenRetries) {
        waitForContainerToStop(waitBetweenRetries, false)
    }

    def waitForContainerToStop(int waitBetweenRetries, boolean printOutput) {
        while (isContainerRunning()) {
            if (printOutput) println "container is still running"
            sleep(waitBetweenRetries)
        }
    }

    /**
     * Check if container is still running.
     * @param containerId
     * @return the state of the container
     */
    def isContainerRunning() {
        return new Boolean(getContainerInfo().State.Running)
    }

    def getContainerInfo() {
        return dockerServer.get("/containers/$id/json", ContentType.JSON)
    }

    DockerContainer withPrivileges() {
        this.isPriveleged = true
        return this
    }

    DockerContainer addCommand(String command) {
        this.commands.add(command)
        return this
    }

    def containerConfiguration() {
        JsonBuilder json = new JsonBuilder()
        json hostname: "",
                Domainname: "",
                User: "",
                Memory: 0,
                MemorySwap: 0,
                CpuShares: 512,
                Cpuset: "0",
                AttachStdin: true,
                AttachStdout: true,
                AttachStderr: true,
                PortSpecs: null,
                Tty: false,
                OpenStdin: false,
                StdinOnce: false,
                Env: [
                        "LOG=file",
                        "DOCKER_DAEMON_ARGS=--insecure-registry artifactory.local:5001"
                ],
                Cmd: commands.join(";"),
                "Image": image,
                "Volumes": [
                        "/tmp": {}
                ],
                "WorkingDir": "",
                "NetworkDisabled": false,
                "ExposedPorts": [
                        "22/tcp": {}
                ]

        return json
    }
}
