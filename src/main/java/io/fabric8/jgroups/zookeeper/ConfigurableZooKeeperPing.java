package io.fabric8.jgroups.zookeeper;

import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Bela Ban
 */
@MBean(description="ZooKeeper based discovery protocol. Acts as a ZooKeeper client and accesses ZooKeeper servers " +
  "to fetch discovery information")
public class ConfigurableZooKeeperPing extends AbstractZooKeeperPing {
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

    static {
        ClassConfigurator.addProtocol(Constants.CONFIGURABLE_ZK_PING_ID, ConfigurableZooKeeperPing.class);
    }

    @Override
    public void init() throws Exception {
        if (connection == null || connection.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing connection property!");
        }
        super.init();
    }

    @Override
    public void start() throws Exception {
        curator.start();
        try {
            super.start();
        } catch (Exception e) {
            curator.close();
            throw e;
        }
    }

    @Override
    public void stop() {
        try {
            removeLocalNode();
        } finally {
            try {
                super.stop();
            } finally {
                curator.close();
            }
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
