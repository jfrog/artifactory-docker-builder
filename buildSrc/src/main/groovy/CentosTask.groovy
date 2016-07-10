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
import org.gradle.api.tasks.TaskAction
import org.jfrog.util.docker.configurations.DockerFileBuilder

/**
 * Created by matank on 7/9/15.
 */
class CentosTask extends BaseTask{

    CentosTask() {
        description = "Build CENTOS ready to work with artifactory"
    }

    @TaskAction
    void build() {
        createDockerFile()
        buildCentosImage()
    }

    @Override
    void createDockerFile() {
        dfb = new DockerFileBuilder(project.projectDir.absolutePath + "/tmp")

        dfb.from centosBaseImage.getFullImageName()
        dfb.label "This is the official Centos image"
        dfb.maintainer "matank@jfrog.com"
        dfb.label("Add Artifactory repositories")
        dfb.add reposFilesToCopy(), "/etc/yum.repos.d/"
        dfb.run "yum update -y"
        dfb.run "rm -rf /etc/yum.repos.d/Centos* && \\\n\
yum install java-1.8.0-openjdk-devel rsync net-tools -y && \\\n\
yum clean all" //Install pre-requisites

        dfb.create()
    }

    void buildCentosImage() {
        try{
            centosImage.inspect()
        } catch (HttpResponseException hre) {
            dockerClient.build(dfb, centosImage)
        }
        println dfb.getDockerfile().text
        dfb.close() //Close DockerFileBuilder to remove any leftovers files from the build process
    }

    private def reposFilesToCopy() {
        def filesFullPaths = []
        new File(this.getClass().getResource("yum/repos").path).listFiles().each {
            filesFullPaths.add(it.absolutePath)
        }
        return filesFullPaths
    }
}
