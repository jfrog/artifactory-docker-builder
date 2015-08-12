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

import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.gradle.api.tasks.TaskAction
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClient
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.util.docker.DockerImage

/**
 * Created by eli on 4/15/15.
 */
class BintrayPushTask extends BaseTask {
    boolean pushLatestTag = false
    RESTClient rClient = null
    Artifactory artifactory = null
    String bintrayRepo = null
    String bintraySubject = null
    String artifactoryRepo = null
    String dockerNamespace = null

    def applicationNames = ['artifactory-pro', 'artifactory-oss', 'artifactory-registry']

    BintrayPushTask() {
        description = 'Push tag/latest from artifactory to Bintray'
    }

    @TaskAction
    void deploy() {
        String user = project.hasProperty('artifactory_user') ? project.getProperty('artifactory_user') : System.getenv("artifactory_user")
        String pass = project.hasProperty('artifactory_password') ? project.getProperty('artifactory_password') : System.getenv("artifactory_password")
        String dockerRegistry = project.hasProperty('artifactory_contextUrl') ? project.getProperty('artifactory_contextUrl') : System.getenv("artifactory_contextUrl")
        artifactory = ArtifactoryClient.create(dockerRegistry, user, pass)
        applicationNames.each { it ->
            DockerImage image = dockerClient.image().registry(registry).namespace(dockerNamespace).repository(it).tag(artifactoryVersion)
            if (imageExistsRemotely(image)) {
                pushToBintray(image)
                if (pushLatestTag) {
                    pushToBintray(image, true)
                }
            }
        }
    }


    def pushToBintray(DockerImage image, boolean asLatest = false) {
        if (asLatest) {
            image.tag("latest")
        }
        println "Publishing to bintray image: ${image.getFullImageName()}"
        String name = image.getNamespace()+"/"+image.getRepository()+":"+image.getTag()
        String path = "api/bintray/docker/push/$artifactoryRepo"
        ArtifactoryRequest ar = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.POST)
                .apiUrl(path)
                .requestType(ArtifactoryRequest.ContentType.JSON)
                .requestBody([dockerImage: name, bintraySubject: bintraySubject, bintrayRepo: bintrayRepo, async: false])
        try {
            artifactory.restCall(ar)
        } catch (HttpResponseException e) {
            System.err.println("Couldn't publish ${image.getFullImageName()} to Bintray")
            System.err.println(e.response.getStatusLine())
            System.err.println(e.response.getData().text)
        }

    }

    boolean imageExistsRemotely(DockerImage image) {
        //The line below suits for docker v2
        //def manifest = artifactory.repository(artifactoryRepo).download("${image.getNamespace()}/${image.getRepository()}/${image.getTag()}/manifest.json").doDownload()
        //The line below suits for docker v1
        def manifest = artifactory.repository(artifactoryRepo).download("repositories/${image.getNamespace()}/${image.getRepository()}/${image.getTag()}/tag.json").doDownload()
        if (manifest) {
            println "${image.getFullImageName()} successfully pulled from artifactory docker repository."
            return true
        }
        return false
    }

    @Override
    void createDockerFile() {

    }

    void setBintrayRepo(String bintrayRepo) {
        this.bintrayRepo = bintrayRepo
    }

    void setArtifactoryRepo(String artifactoryRepo) {
        this.artifactoryRepo = artifactoryRepo
    }

    void setBintraySubject(String bintraySubject) {
        this.bintraySubject = bintraySubject
    }

    void setDockerNamespace(String dockerNamespace) {
        this.dockerNamespace = dockerNamespace
    }
}
