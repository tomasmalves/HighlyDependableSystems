package test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import communication.AuthenticatedPerfectLink;
import consensus.ConsensusNode;

public class ConsensusTest {
    public static void main(String[] args) throws Exception {

    	Map<Integer, InetSocketAddress> peers = new HashMap<>();
    	peers.put(2, new InetSocketAddress("192.168.1.102", 5002));  
    	peers.put(3, new InetSocketAddress("192.168.1.103", 5003));  
    	peers.put(4, new InetSocketAddress("192.168.1.104", 5004));

    	AuthenticatedPerfectLink apl = new AuthenticatedPerfectLink(1, 5001, "superSecretKey123", peers);
    	ConsensusNode node1 = new ConsensusNode(1, apl);
    }
}
