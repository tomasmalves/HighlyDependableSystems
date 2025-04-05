package consensus;

import java.security.PublicKey;

/**
 * Information about a process in the system
 */
public class ProcessInfo {
    private final String host;
    private final int port;
    private final PublicKey publicKey;

    public ProcessInfo(String host, int port, PublicKey publicKey) {
        this.host = host;
        this.port = port;
        this.publicKey = publicKey;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}