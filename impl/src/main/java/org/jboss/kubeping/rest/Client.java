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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.PingData;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Client {
    private String rootURL;
    private Certs certs;

    protected Client() {
    }

    public Client(String host, String port, String version, Certs certs) throws MalformedURLException {
        final String protocol = (certs != null) ? "https" : "http";
        this.rootURL = String.format("%s://%s:%s/api/%s", protocol, host, port, version);
        this.certs = certs;
    }

    private InputStream openStream(String url, int tries, long sleep) {
        final int attempts = tries;
        Throwable lastFail = null;
        while (tries > 0) {
            tries--;
            try {
                if (certs != null) {
                    return certs.openStream(url);
                } else {
                    URL xurl = new URL(url);
                    return xurl.openStream();
                }
            } catch (Throwable fail) {
                lastFail = fail;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        String emsg = String.format("%s attempt(s) to open stream [%s] failed. Last failure was [%s: %s].",
                attempts, url,
                (lastFail != null ? lastFail.getClass().getName() : "null"),
                (lastFail != null ? lastFail.getMessage() : ""));
        throw (lastFail != null) ? new IllegalStateException(emsg, lastFail) : new IllegalStateException(emsg);
    }

    public String info() {
        return "Kubernetes master URL: " + rootURL;
    }

    protected ModelNode getNode(String op) throws IOException {
        return getNode(op, null);
    }

    protected ModelNode getNode(String op, String labelsQuery) throws IOException {
        String url = rootURL + "/" + op;
        if (labelsQuery != null && labelsQuery.length() > 0) {
            url += "?labels=" + URLEncoder.encode(labelsQuery, "UTF-8");
        }
        try (InputStream stream = openStream(url, 60, 1000)) {
            return ModelNode.fromJSONStream(stream);
        }
    }

    public List<Pod> getPods() throws IOException {
        return getPods(null);
    }

    public List<Pod> getPods(String labelsQuery) throws IOException {
        ModelNode root = getNode("pods", labelsQuery);
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

            ModelNode ctns = manifest.get("containers");
            if (ctns.isDefined() == false) continue;

            List<ModelNode> containers = ctns.asList();
            for (ModelNode c : containers) {
                Container container = new Container(pod.getHost(), pod.getPodIP());
                String cname = c.get("name").asString();
                container.setName(cname);

                ModelNode pts = c.get("ports");
                if (pts.isDefined() == false) continue;

                List<ModelNode> ports = pts.asList();
                for (ModelNode p : ports) {
                    String pname = p.get("name").asString();
                    Port port = new Port(pname,
                            p.get("hostPort").isDefined() ? p.get("hostPort").asInt() : null,
                            p.get("containerPort").isDefined() ? p.get("containerPort").asInt() : null);
                    container.addPort(port);
                }

                pod.addContainer(container);
            }

            pods.add(pod);
        }
        return pods;
    }

    public boolean accept(Context context) {
        Container container = context.getContainer();
        List<Port> ports = container.getPorts();
        if (ports != null) {
            String pingPortName = context.getPingPortName();
            for (Port port : ports) {
                if (pingPortName.equalsIgnoreCase(port.getName())) {
                    return true;
                }
            }
        }
        return false;
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
