package blockchain;

import java.net.InetAddress;
import communication.NetworkHandler;

public class ClientLibrary {
	private final NetworkHandler networkHandler;
    private final InetAddress serverAddress;
    private final int serverPort;
    
    public ClientLibrary(String serverHost, int serverPort) throws Exception {
        this.networkHandler = new NetworkHandler(0); // Let OS assign port
        this.serverAddress = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;
    }
    
    public void sendTransaction(String transaction) throws Exception {
        networkHandler.sendMessage("APPEND|" + transaction, serverAddress, serverPort);
        System.out.println("Transaction sent: " + transaction);
    }
}
