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

package org.jgroups.protocols;

import io.fabric8.jgroups.zookeeper.ConfigurableZooKeeperPing;
import io.fabric8.jgroups.zookeeper.Constants;
import io.fabric8.jgroups.zookeeper.Fabric8ACLProvider;
import io.fabric8.jgroups.zookeeper.PasswordEncoder;
import org.jgroups.conf.ClassConfigurator;

/**
 * A workaround "org.jgroups.protocols" prefix limitation.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ZKPING extends ConfigurableZooKeeperPing {
    static {
        ClassConfigurator.addProtocol(Constants.WF_ZK_PING_ID, ZKPING.class);
    }

    @Override
    public void init() throws Exception {
        // connection url
        String zkURL = System.getenv("FABRIC8_ZOOKEEPER_URL");
        if (zkURL != null) {
            connection = zkURL;
        }

        // password
        String zkPassword = System.getenv("FABRIC8_ZOOKEEPER_PASSWORD");
        if (zkPassword != null) {
            password = PasswordEncoder.decode(zkPassword);
            setAclProvider(new Fabric8ACLProvider());
        }

        super.init();
    }

    @Override
    protected byte[] getAuth() {
        return ("fabric:" + password).getBytes();
    }
}
