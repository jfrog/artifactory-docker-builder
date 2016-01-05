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

import BaseSpec
import groovyx.net.http.HttpResponseException
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl

class DockerProSpec extends BaseSpec {

    @Override
    def waitForArtifactory() {
        artifactoryAdmin = artifactoryClient.create("http://${getHostFromDockerUrl()}:8081/artifactory/", "admin", "password")
        int retries = 40
        while (retries > 0) {
            try {
                saveLicense()
                break
            } catch (Exception e) {
                if (retries-- == 0) {
                    throw new RuntimeException('fail to save license', e)
                } else {
                    sleep 1000
                }
            }
        }
    }

    def saveLicense() {
        def lic = this.getClass().getResource("artifactory.lic").text
        ArtifactoryRequest ar = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.POST)
                .apiUrl("api/system/license")
                .requestType(ArtifactoryRequest.ContentType.JSON)
                .requestBody([licenseKey: lic])
                .responseType(ArtifactoryRequest.ContentType.JSON)
        try {
            artifactoryAdmin.restCall(ar)
        } catch (ConnectException con) {
            println 'fail to save license'
            throw new RuntimeException('fail to save license', con)
        } catch (HttpResponseException ed) {
            throw new RuntimeException('fail to save license', ed)
        }
    }

    @Override
    String getDockerRepository() {
        return "artifactory-pro"
    }
}