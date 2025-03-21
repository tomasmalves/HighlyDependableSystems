package communication;

import java.net.*;
import java.nio.charset.StandardCharsets;

//DONE
public class AuthenticatedPerfectLink {
    private final int nodeId;
    private final int port;
    private final DatagramSocket socket;
    private final int targetNodeId;   
    private final InetSocketAddress targetAddress; 

    
    public AuthenticatedPerfectLink(int nodeId, int port, int targetNodeId , InetSocketAddress targetAddress) throws Exception {
        
    	this.nodeId = nodeId;
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.targetNodeId = targetNodeId;
        this.targetAddress = targetAddress;
  
    }

    public void send(String message) throws Exception {
    	
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
        socket.send(packet);
        System.out.println("Node " + nodeId + " sent message to port " + targetAddress.getPort());
    }

    public String receive() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String receivedMessage = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        
        System.out.println("Node " + nodeId + " received message: " + receivedMessage);
        
        
        return receivedMessage;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getPort() {
        return port;
    }
}