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

package org.jboss.kubeping;

import java.util.ArrayList;
import java.util.List;

import org.jboss.kubeping.rest.Client;
import org.jboss.kubeping.rest.Container;
import org.jboss.kubeping.rest.Pod;
import org.jboss.kubeping.rest.Server;
import org.jboss.kubeping.rest.ServerFactory;
import org.jboss.kubeping.rest.Utils;
import org.jgroups.Address;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@MBean(description = "Kubernetes based discovery protocol")
public class KubePing extends FILE_PING {
    static {
        ClassConfigurator.addProtocol(Constants.KUBE_PING_ID, KubePing.class);
    }

    @Property
    private String host;

    @Property
    private String port;

    @Property
    private String version;

    @Property
    private int serverPort;

    private ServerFactory factory;
    private Server server;
    private Client client;

    public void setFactory(ServerFactory factory) {
        this.factory = factory;
    }

    private String getHost() {
        if (host != null) {
            return host;
        } else {
            return System.getenv("KUBERNETES_RO_SERVICE_HOST");
        }
    }

    private String getPort() {
        if (port != null) {
            return port;
        } else {
            return System.getenv("KUBERNETES_RO_SERVICE_PORT");
        }
    }

    private String getVersion() {
        if (version != null) {
            return version;
        } else {
            return "v1beta1";
        }
    }

    private int getServerPort() {
        if (serverPort > 0) {
            return serverPort;
        } else {
            return 8888;
        }
    }

    @Override
    public void start() throws Exception {
        client = new Client(getHost(), getPort(), getVersion());
        log.info(client.info());

        if (factory != null) {
            server = factory.create(getServerPort(), stack.getChannel());
        } else {
            server = Utils.createServer(getServerPort(), stack.getChannel());
        }
        server.start();
    }

    @Override
    public void stop() {
        try {
            server.stop();
        } finally {
            super.stop();
        }
    }

    /**
     * Reads all information from the given directory under clustername
     *
     * @return all data
     */
    protected synchronized List<PingData> readAll(String clusterName) {
        List<PingData> retval = new ArrayList<>();
        try {
            List<Pod> pods = client.getPods();
            for (Pod pod : pods) {
                List<Container> containers = pod.getContainers();
                for (Container container : containers) {
                    if (client.accept(container)) {
                        retval.add(client.getPingData(container.getPodIP(), container.getPort("ping").getContainerPort()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn(String.format("Failed to read ping data from Kubernetes [%s] for cluster: %s", client.info(), clusterName), e);
        }
        return retval;
    }

    @Override
    protected void createRootDir() {
        // empty on purpose to prevent dir from being created in the local file system
    }

    @Override
    protected void writeToFile(PingData data, String clustername) {
    }

    @Override
    protected void remove(String clustername, Address addr) {
    }
}
