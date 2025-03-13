package blockchain;

import java.net.InetAddress;
import communication.NetworkHandler;

public class ClientLibrary {
    private final NetworkHandler networkHandler;
    private final InetAddress leaderAddress;
    private final int leaderPort;

    public ClientLibrary(String leaderHost, int leaderPort) throws Exception {
        this.networkHandler = new NetworkHandler(0); // O sistema de rede vai atribuir a porta
        this.leaderAddress = InetAddress.getByName(leaderHost);
        this.leaderPort = leaderPort;
    }

    // Método que o cliente usa para enviar uma proposta de valor ao líder
    public void sendConsensusRequest(String value) throws Exception {
        String message = "CONSENSUS_REQUEST|" + value;
        networkHandler.sendMessage(message, leaderAddress, leaderPort);
        System.out.println("Sent consensus request: " + value);
    }
}
