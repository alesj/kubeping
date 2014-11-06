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

import java.io.DataOutputStream;
import java.util.Collections;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.protocols.PingData;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Server {
    private final int port;
    private final Channel channel;
    private Undertow undertow;

    public Server(int port, Channel channel) {
        this.port = port;
        this.channel = channel;
    }

    public void start() {
        Undertow.Builder builder = Undertow.builder();
        builder.addHttpListener(port, "127.0.0.1");
        builder.setHandler(new Handler());
        undertow = builder.build();
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }

    public static PingData createPingData(Channel channel) {
        Address address = channel.getAddress();
        View view = channel.getView();
        boolean is_server = false;
        String logical_name = channel.getName();
        PhysicalAddress paddr = (PhysicalAddress) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, address));

        return new PingData(address, view, is_server, logical_name, Collections.singleton(paddr));
    }

    private class Handler implements HttpHandler {
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.startBlocking();

            PingData data = createPingData(channel);
            data.writeTo(new DataOutputStream(exchange.getOutputStream()));
        }
    }
}
