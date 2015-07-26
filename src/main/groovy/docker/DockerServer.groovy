package docker

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by matank on 12/21/2014.
 */
class DockerServer {
    protected static Logger logger = Logger.getLogger('DockerServer')

    def url
    def address
    static RESTClient restClient
    def portBindings = [
            '8081/tcp': [[HostIp: '0.0.0.0', HostPort: '8081']],
            '80/tcp'  : [[HostIp: '0.0.0.0', HostPort: '80']],
            '443/tcp' : [[HostIp: '0.0.0.0', HostPort: '443']],
            '5001/tcp': [[HostIp: '0.0.0.0', HostPort: '5001']],
            '5002/tcp': [[HostIp: '0.0.0.0', HostPort: '5002']]
    ]

    DockerServer(url) {
        this.url = url
        this.address = url.find(/http:\/\/(.*):.*/) { match -> return match[1] }
        restClient = new RESTClient(this.url)
    }

    /**
     * Creates container based on the image provided.
     * @param imageName - the image to create the container based on
     * @param commands - the command to execute, in case there are arguments, pass a list containing all the items ["java", "-version"]
     * @param binds - mounting host machine to the created container, should be in the list "[host_path:container_path]"
     * @return the ID of the container
     */
    def createContainer(String imageName, def commands) {

        if (commands instanceof List) commands = commands.join(';')

        def config = containerConfiguration(imageName, [commands])
        if (portBindings != null) {
            JsonSlurper jsonSlurper = new JsonSlurper()
            def tmpConfig = jsonSlurper.parseText(config)
            tmpConfig.HostConfig.PortBindings = portBindings
            config = JsonOutput.toJson(tmpConfig)
        }
        try {
            HttpResponseDecorator result = restClient.post(
                    path: "/containers/create",
                    body: config,

                    requestContentType: ContentType.JSON
            )

            return new DockerContainer([dockerServer: this, id: result.getData().Id])
        } catch (HttpResponseException hre) {
            logger.info "Response Message: ${hre.getMessage()}"
            logger.info "Response Body: ${hre.response.getData().text}"
            throw hre
        }
    }


    def getNewContainer(String image) {
        return new DockerContainer(this, image)
    }

    def get(path, contentType) {
        def string = restClient.get(
                path: path,
                contentType: contentType
        )

        return string.getData()
    }

    def getContainerId(image, command) {
        HttpResponseDecorator result = restClient.get(
                path: "/containers/json",
                query: [all: 1],
                contentType: ContentType.JSON
        )

        def containers = result.getData()

        def toReturn = null
        for (int i = 0; i < containers.size(); i++) {
            if ((containers[i].Image == image) && (containers[i].Command == command)) {
                return containers[i].Id
            }
        }
    }

    def createTag(String orgImageName, String orgTag, String newImageName, String newTag) {
        logger.info("Creating $newImageName:$newTag tag")
        def restClient = getRestClient()
        try {
            restClient.post(
                    path: "/images/$orgImageName:$orgTag/tag",
                    query: [repo: "$newImageName", tag: newTag, force: 1],
                    requestContentType: ContentType.JSON
            )
        } catch (HttpResponseException e) {
            logger.log(Level.SEVERE,"Couldn't create $newImageName:$newTag")
            logger.log(Level.SEVERE,e.response.getStatusLine().reasonPhrase)
            logger.log(Level.SEVERE,e.response.getData().text,e)
            throw new RuntimeException(e)
        }

    }

    def pushImage(String imageName, String tag, String user, String pass) {
        logger.info("Pushing image: $imageName:$tag")
        def builder = new groovy.json.JsonBuilder()
        builder {
            username user
            password pass
            auth ''
            email ''
        }
        String auth = builder.toString()
        pushImage(imageName, tag, auth)
    }

    def pushImage(String imageName, String tag, String authJson) {
        logger.info("Pushing image: $imageName:$tag")
        String header = authJson.bytes.encodeBase64()
        getRestClient().post(
                path: "/images/$imageName/push",
                query: [tag     : tag,
                        registry: ""],
                headers: ["X-Registry-Auth": header]
        )
    }


    def containerConfiguration(imageName, commands) {
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
                        "LOG=file"
                ],
                Cmd: commands,
                "Image": imageName,
                "Volumes": [
                        "/tmp": {}
                ],
                "WorkingDir": "",
                "NetworkDisabled": false,
                "ExposedPorts": [
                        "22/tcp": {}
                ],
                "HostConfig": [
                        "Binds"          : [],
                        "Links"          : [],
                        "PortBindings"   : ["8081/tcp": [["HostPort": "8081"]]],
                        "PublishAllPorts": false,
                        "Privileged"     : false,
                        "Dns"            : ["8.8.8.8"],
                        "DnsSearch"      : [""],
                        "VolumesFrom"    : [],
                        "CapAdd"         : ["NET_ADMIN"],
                        "CapDrop"        : ["MKNOD"],
                        "RestartPolicy"  : ["Name": "", "MaximumRetryCount": 0],
                        "NetworkMode"    : "bridge",
                        "Devices"        : []
                ]

        return json.toString()
    }
}
