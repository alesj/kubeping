package io.fabric8.jgroups.zookeeper;

import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConfigurableZooKeeperPing extends AbstractZooKeeperPing {
    @Property(name = "name")
    private String logical_name;

    @Property
    private String connection;

    @Property
    private int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT_MS;

    @Property
    private int sessionTimeout = Constants.DEFAULT_SESSION_TIMEOUT_MS;

    @Property
    private int maxRetry = Constants.MAX_RETRIES_LIMIT;

    @Property
    private int retryInterval = Constants.DEFAULT_RETRY_INTERVAL;

    static {
        ClassConfigurator.addProtocol(Constants.CONFIGURABLE_ZK_PING_ID, ConfigurableZooKeeperPing.class);
    }

    protected String provideLogicalName() {
        return logical_name;
    }

    @Override
    public void init() throws Exception {
        if (logical_name == null || logical_name.trim().length() == 0) {
            throw new IllegalArgumentException("Missing logical_name property!");
        }
        if (connection == null || connection.trim().length() == 0) {
            throw new IllegalArgumentException("Missing connection property!");
        }
        super.init();
    }

    @Override
    public void start() throws Exception {
        super.start();
        try {
            getCurator().start();
        } catch (Exception e) {
            super.stop();
            throw e;
        }
    }

    @Override
    public void stop() {
        try {
            getCurator().close();
        } finally {
            super.stop();
        }
    }

    protected CuratorFramework createCuratorFramework() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .ensembleProvider(new FixedEnsembleProvider(connection))
            .connectionTimeoutMs(connectionTimeout)
            .sessionTimeoutMs(sessionTimeout)
            .retryPolicy(new RetryNTimes(maxRetry, retryInterval));
        return builder.build();
    }
}
