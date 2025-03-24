package consensus;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TestClient {
    public static void sendTestMessage(String message) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("localhost");

        byte[] data = message.getBytes();

        for (int i = 1; i <= 4; i++) {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 5000 + i);
            socket.send(packet);
        }

        System.out.println("Test client sent: " + message);
    }
}
