package io.fabric8.jgroups.zookeeper;

import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.FILE_PING;
import org.jgroups.protocols.PingData;
import org.jgroups.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Ioannis Canellos
 * @author Bela Ban
 */
@MBean(description="ZooKeeper based discovery protocol. Acts as a ZooKeeper client and accesses ZooKeeper servers " +
  "to fetch discovery information")
public class ConfigurableZooKeeperPing extends FILE_PING {
    @Property
    protected String connection;

    @Property
    protected int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT_MS;

    @Property
    protected int sessionTimeout = Constants.DEFAULT_SESSION_TIMEOUT_MS;

    @Property
    protected int maxRetry = Constants.MAX_RETRIES_LIMIT;

    @Property
    protected int retryInterval = Constants.DEFAULT_RETRY_INTERVAL;

    protected static final String ROOT_PATH="/fabric/registry/jgroups/";
    protected String              DISCOVERY_PATH;
    protected String              LOCAL_NODE_PATH;

    protected CuratorFramework    curator;



    static {
        ClassConfigurator.addProtocol(Constants.CONFIGURABLE_ZK_PING_ID, ConfigurableZooKeeperPing.class);
    }




    @Override
    public void init() throws Exception {
        if (connection == null || connection.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing connection property!");
        }
        curator=createCurator();
        super.init();
    }

    @Override
    public void start() throws Exception {
        super.start();
        curator.start();
    }

    @Override
    public void stop() {
        removeLocalNode();
        curator.close();
        super.stop();
    }

    public Object down(Event evt) {
        switch(evt.getType()) {
            case Event.CONNECT:
            case Event.CONNECT_USE_FLUSH:
            case Event.CONNECT_WITH_STATE_TRANSFER:
            case Event.CONNECT_WITH_STATE_TRANSFER_USE_FLUSH:
                DISCOVERY_PATH=ROOT_PATH + evt.getArg();
                LOCAL_NODE_PATH=DISCOVERY_PATH + "/" + addressAsString(local_addr);
                _createRootDir();
                break;
        }
        return super.down(evt);
    }

    /** Creates the root node in ZooKeeper (/fabric/registry/jgroups */
    @Override
    protected void createRootDir() {
        ; // empty on purpose to prevent dir from being created in the local file system
    }

    protected void _createRootDir() {
        try {
            if(curator.checkExists().forPath(LOCAL_NODE_PATH) == null) {
                curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(LOCAL_NODE_PATH);
            }
        }
        catch(Exception e) {
            throw new RuntimeException("failed to create dir " + LOCAL_NODE_PATH + " in ZooKeeper",e);
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
            String clusterPath = DISCOVERY_PATH;
            for (String node : curator.getChildren().forPath(clusterPath)) {
                String nodePath = ZKPaths.makePath(clusterPath,node);
                PingData nodeData = readPingData(nodePath);
                if(nodeData != null)
                    retval.add(nodeData);
            }

        } catch (Exception e) {
            log.debug(String.format("Failed to read ping data from ZooKeeper for cluster: %s", clusterName), e);
        }
        return retval;
    }


    @Override
    protected synchronized void writeToFile(PingData data, String clustername) {
        writePingData(data);
    }



    protected synchronized PingData readPingData(String path) {
        PingData retval = null;
        DataInputStream in = null;
        try {
            byte[] bytes = curator.getData().forPath(path);
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

    protected synchronized void writePingData(PingData data) {
        String nodePath = LOCAL_NODE_PATH;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(baos);

            data.writeTo(dos);

            if (curator.checkExists().forPath(nodePath) == null) {
                curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(LOCAL_NODE_PATH, baos.toByteArray());
            } else {
                curator.setData().forPath(LOCAL_NODE_PATH, baos.toByteArray());
            }
        } catch (Exception ex) {
            log.error("Error saving ping data", ex);
        } finally {
            Util.close(dos);
            Util.close(baos);
        }
    }

    protected void removeLocalNode() {
        try {
            curator.delete().forPath(LOCAL_NODE_PATH);
        }
        catch(Exception e) {
            log.error("failed removing " + LOCAL_NODE_PATH,e);
        }
    }

    protected void remove(String clustername, Address addr) {
        String nodePath=DISCOVERY_PATH + "/" + addressAsString(addr);
        try {
            curator.delete().forPath(nodePath);
        }
        catch(Exception e) {
            log.error("failed removing " + nodePath, e);
        }
    }

    protected CuratorFramework createCurator() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .ensembleProvider(new FixedEnsembleProvider(connection))
            .connectionTimeoutMs(connectionTimeout)
            .sessionTimeoutMs(sessionTimeout)
            .retryPolicy(new RetryNTimes(maxRetry, retryInterval));
        return builder.build();
    }
}
