package consensus;

import java.net.*;
import java.util.Scanner;

public class ConsensusClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java ConsensusClient <server-port> <message>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String message = args[1];

        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("localhost");

        byte[] buffer = message.getBytes();

        DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, address, port);

        System.out.println("Sending '" + message + "' to port " + port);
        socket.send(packet);

        // Wait for response
        buffer = new byte[1024];
        packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String response = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received response: " + response);

        socket.close();
    }
}