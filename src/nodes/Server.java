package nodes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server {

    private DatagramSocket clientSocket;
    private DatagramSocket leaderSocket;
    private byte[] clientsBuffer;
    private byte[] leadersBuffer;

    public Server(DatagramSocket clientSocket, DatagramSocket leaderSocket) {
        this.clientSocket = clientSocket;
        this.leaderSocket = leaderSocket;
<<<<<<< HEAD
=======
        initProcesses();
    }

    private void initProcesses() {
>>>>>>> 8601c7e31b0957168b8816505a216341908d9d7b
    }

    public void receiveThenSend() {
        while (true) {
            try {
                // Reset buffer for each new message
                clientsBuffer = new byte[1024];
                leadersBuffer = new byte[1024];

                // Receive message from client
                DatagramPacket clientsDatagramPacket = new DatagramPacket(clientsBuffer, clientsBuffer.length);
                clientSocket.receive(clientsDatagramPacket);
                InetAddress clientsAddress = clientsDatagramPacket.getAddress();
                int clientsPort = clientsDatagramPacket.getPort();
                String messageFromClient = new String(clientsDatagramPacket.getData(), 0,
                        clientsDatagramPacket.getLength());
                System.out.println("Message from client: " + messageFromClient);

                // Forward message to leader
                byte[] messageToLeaderBytes = messageFromClient.getBytes();
                InetAddress leadersAddress = InetAddress.getByName("localhost");
                DatagramPacket leaderDatagramPacket = new DatagramPacket(messageToLeaderBytes,
                        messageToLeaderBytes.length, leadersAddress, 5001);
                leaderSocket.send(leaderDatagramPacket);

                // Print debugging information
                System.out.println(
                        "Server sent message to leader at port 5001 from local port " + leaderSocket.getLocalPort());

                // Receive response from leader
                leaderDatagramPacket = new DatagramPacket(leadersBuffer, leadersBuffer.length);
                leaderSocket.receive(leaderDatagramPacket);
                String messageFromLeader = new String(leaderDatagramPacket.getData(), 0,
                        leaderDatagramPacket.getLength());
                System.out.println("Leader says: " + messageFromLeader);

                // Send leader's response back to client
                byte[] messageToClientBytes = messageFromLeader.getBytes();
                clientsDatagramPacket = new DatagramPacket(messageToClientBytes, messageToClientBytes.length,
                        clientsAddress, clientsPort);
                clientSocket.send(clientsDatagramPacket);

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) throws SocketException {
        DatagramSocket clientSocket = new DatagramSocket(5000);
        DatagramSocket leaderSocket = new DatagramSocket();
        Server server = new Server(clientSocket, leaderSocket);
        server.receiveThenSend();
    }
}