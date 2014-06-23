package io.fabric8.test.jgroups.zookeeper;

import org.jgroups.protocols.ZKPING;
import org.junit.Test;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ZkPingTest {

    ZKPING ping;

    @Test
    public void testRead() throws Exception {
        /*
        ping = new ZKPING();
        ping.init();
        try {
            List<PingData> pings = ping.pings("capedwarf");
            System.out.println("pings = " + pings);
        } finally {
            ping.stop();
            ping.destroy();
        }
        */
    }
}
