package communication;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class NetworkHandler {
	private final DatagramSocket socket;
    private final int port;
    
    public NetworkHandler(int port) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
    }
    
    public void sendMessage(String message, InetAddress address, int targetPort) throws Exception {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, targetPort);
        socket.send(packet);
    }
    
    public String receiveMessage() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }
}
