package service;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;

import consensus.Client;
import consensus.ConsensusNode;

public class BlockchainService {
    // private final HashMap<Integer, ConsensusNode> processes;
    private DatagramSocket datagramSocket;
    private List<Client> clients;
    private String block;

    public BlockchainService(Client client, String block) throws SocketException {
        this.datagramSocket = new DatagramSocket();
        this.block = block;
        this.clients.add(client);
        // this.processes = this.initProcesses(block);
    }

    /*
     * private HashMap<Integer, ConsensusNode> initProcesses(String block) {
     * for (int i = 1; i <= 4; i++) {
     * processes.put(i, new ConsensusNode(null, null));
     * }
     * }
     */

}
