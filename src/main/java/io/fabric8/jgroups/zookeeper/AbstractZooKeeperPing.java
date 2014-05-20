package io.fabric8.jgroups.zookeeper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.jgroups.Event;
import org.jgroups.PhysicalAddress;
import org.jgroups.annotations.Property;
import org.jgroups.protocols.Discovery;
import org.jgroups.protocols.PingData;
import org.jgroups.util.Util;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Ioannis Canellos
 */
public abstract class AbstractZooKeeperPing extends Discovery {
    private static final String DISCOVERY_PATH = "/fabric/registry/jgroups/%s";
    private static final String NODE_PATH = DISCOVERY_PATH + "/%s";

    private String logicalName;
    private volatile CuratorFramework curator;

    private Future<?> writer_future;

    @SuppressWarnings("FieldCanBeLocal")
    @Property(description = "Writer interval.")
    private long interval = 60 * 1000L;

    protected abstract String provideLogicalName();

    protected abstract CuratorFramework createCuratorFramework();

    protected String getLogicalName() {
        if (logicalName == null) {
            logicalName = provideLogicalName();
        }
        return logicalName;
    }

    protected CuratorFramework getCurator() {
        if (curator == null) {
            synchronized (this) {
                if (curator == null) {
                    curator = createCuratorFramework();
                }
            }
        }
        return curator;
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void start() throws Exception {
        super.start();
        if (interval > 0) {
            writer_future = timer.scheduleWithFixedDelay(new WriterTask(), interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (writer_future != null) {
            writer_future.cancel(false);
            writer_future = null;
        }
        super.stop();
    }

    public Collection<PhysicalAddress> fetchClusterMembers(String clusterName) {
        List<PingData> existing_mbrs = readAll(clusterName);

        writeOwnPing(clusterName);

        if (existing_mbrs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<PhysicalAddress> retval = new HashSet<>();

        for (PingData tmp : existing_mbrs) {
            Collection<PhysicalAddress> dests = tmp != null ? tmp.getPhysicalAddrs() : null;
            if (dests != null) {
                for (final PhysicalAddress dest : dests) {
                    if (dest != null) {
                        retval.add(dest);
                    }
                }
            }
        }

        return retval;
    }

    public boolean sendDiscoveryRequestsInParallel() {
        return true;
    }

    public boolean isDynamic() {
        return true;
    }

    private void writeOwnPing(String clusterName) {
        PhysicalAddress physical_addr = (PhysicalAddress) down(new Event(Event.GET_PHYSICAL_ADDRESS, local_addr));
        List<PhysicalAddress> physical_addrs = Arrays.asList(physical_addr);
        PingData data = new PingData(local_addr, null, false, getLogicalName(), physical_addrs);
        writePingData(data, clusterName);
    }

    /**
     * Reads all information from the given directory under clustername
     *
     * @return all data
     */
    protected synchronized List<PingData> readAll(String clusterName) {
        List<PingData> retval = new ArrayList<>();
        try {
            String clusterPath = String.format(DISCOVERY_PATH, clusterName);
            for (String node : getCurator().getChildren().forPath(clusterPath)) {
                String nodePath = ZKPaths.makePath(clusterPath, node);
                PingData nodeData = readPingData(nodePath);
                retval.add(nodeData);
            }

        } catch (Exception e) {
            log.debug(String.format("Failed to read ping data from ZooKeeper for cluster: %s", clusterName), e);
        }
        return retval;
    }

    private synchronized PingData readPingData(String path) {
        PingData retval = null;
        DataInputStream in = null;
        try {
            byte[] bytes = getCurator().getData().forPath(path);
            in = new DataInputStream(new ByteArrayInputStream(bytes));
            PingData tmp = new PingData();
            tmp.readFrom(in);
            return tmp;
        } catch (Exception e) {
            log.debug(String.format("Failed to read ZooKeeper node: %s", path), e);
        } finally {
            Util.close(in);
        }
        return retval;
    }

    protected synchronized void writePingData(PingData data, String clusterName) {
        String nodePath = String.format(NODE_PATH, clusterName, getLogicalName());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(baos);

            data.writeTo(dos);

            if (getCurator().checkExists().forPath(nodePath) == null) {
                getCurator().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(String.format(NODE_PATH, clusterName, getLogicalName()), baos.toByteArray());
            } else {
                getCurator().setData().forPath(String.format(NODE_PATH, clusterName, getLogicalName()), baos.toByteArray());
            }
        } catch (Exception ex) {
            log.error(String.format("Error saving ping data."), ex);
        } finally {
            Util.close(dos);
            Util.close(baos);
        }
    }

    protected class WriterTask implements Runnable {
        public void run() {
            writeOwnPing(group_addr);
        }
    }
}
