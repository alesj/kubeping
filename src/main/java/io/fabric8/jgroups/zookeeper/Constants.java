package io.fabric8.jgroups.zookeeper;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class Constants {
    public static final short CONFIGURABLE_ZK_PING_ID = 1001;
    public static final short MANAGED_ZK_PING_ID = 1002;

    public static final String ZOOKEEPER_URL = "zookeeper.url";
    public static final String ZOOKEEPER_PASSWORD = "zookeeper.password";
    public static final String ENSEMBLE_ID = "ensemble.id";

    public static final String SESSION_TIMEOUT = "sessionTimeOutMs";
    public static final String CONNECTION_TIMEOUT = "connectionTimeOutMs";

    public static final String RETRY_POLICY_MAX_RETRIES = "retryPolicy.maxRetries";
    public static final String RETRY_POLICY_INTERVAL_MS = "retryPolicy.retryIntervalMs";

    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 15 * 1000;
    public static final int DEFAULT_SESSION_TIMEOUT_MS = 60 * 1000;
    public static final int MAX_RETRIES_LIMIT = 3;
    public static final int DEFAULT_RETRY_INTERVAL = 500;

}
