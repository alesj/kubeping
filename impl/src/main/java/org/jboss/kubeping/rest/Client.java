/**
 *  Copyright 2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.jboss.kubeping.rest;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.PingData;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Client {
    private String rootURL;

    protected Client() {
    }

    public Client(String host, String port, String version) throws MalformedURLException {
        this.rootURL = String.format("http://%s:%s/api/%s", host, port, version);
    }

    private static InputStream openStream(String url, int tries, long sleep) {
        while (tries > 0) {
            tries--;
            try {
                URL xurl = new URL(url);
                return xurl.openStream();
            } catch (Throwable ignore) {
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException(String.format("Cannot open stream [%s].", url));
    }

    public String info() {
        return "Kubernetes master URL: " + rootURL;
    }

    protected ModelNode getNode(String op) throws IOException {
        try (InputStream stream = openStream(rootURL + "/" + op, 60, 1000)) {
            return ModelNode.fromJSONStream(stream);
        }
    }

    public List<Pod> getPods() throws IOException {
        ModelNode root = getNode("pods");
        List<Pod> pods = new ArrayList<>();
        List<ModelNode> items = root.get("items").asList();
        for (ModelNode item : items) {
            Pod pod = new Pod();

            ModelNode currentState = item.get("currentState");
            ModelNode host = currentState.get("host");
            pod.setHost(host.asString());
            ModelNode podIP = currentState.get("podIP");
            pod.setPodIP(podIP.asString());

            ModelNode desiredState = item.get("desiredState");
            ModelNode manifest = desiredState.get("manifest");
            List<ModelNode> containers = manifest.get("containers").asList();
            for (ModelNode c : containers) {
                Container container = new Container(pod.getHost(), pod.getPodIP());
                String cname = c.get("name").asString();
                container.setName(cname);

                List<ModelNode> ports = c.get("ports").asList();
                for (ModelNode p : ports) {
                    String pname = p.get("name").asString();
                    String hostPort = p.get("hostPort").asString();
                    String containerPort = p.get("containerPort").asString();
                    Port port = new Port(pname, Integer.parseInt(hostPort), Integer.parseInt(containerPort));
                    container.addPort(port);
                }

                pod.addContainer(container);
            }

            pods.add(pod);
        }
        return pods;
    }

    public boolean accept(Container container) {
        return true; // TODO -- filter WF instances
    }

    public PingData getPingData(String host, int port) throws Exception {
        String url = String.format("http://%s:%s", host, port);
        PingData data = new PingData();
        try (InputStream is = openStream(url, 100, 500)) {
            data.readFrom(new DataInputStream(is));
        }
        return data;
    }
}
