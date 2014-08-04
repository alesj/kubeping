package io.fabric8.test.jgroups.zookeeper;

import java.util.List;

import org.jgroups.protocols.PingData;
import org.jgroups.protocols.ZKPING;
import org.junit.Test;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ZkPingTest {

    @Test
    public void testRead() throws Exception {
        LookupPing ping = new LookupPing();
        ping.init();
        try {
            List<PingData> pings = ping.pings("capedwarf");
            System.out.println("pings = " + pings);
        } finally {
            ping.stop();
            ping.destroy();
        }
    }

    private static class LookupPing extends ZKPING {
        private List<PingData> pings(String cluster) {
            return readAll(cluster);
        }
    }
}
