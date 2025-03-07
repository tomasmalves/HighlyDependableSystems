package communication;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthenticatedPerfectLink {
	private final DatagramSocket socket;
    private final InetAddress remoteAddress;
    private final int remotePort;
    private final Key hmacKey;

    public AuthenticatedPerfectLink(int localPort, String remoteHost, int remotePort, String secretKey) throws Exception {
        this.socket = new DatagramSocket(localPort);
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.hmacKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private String computeHMAC(String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(hmacKey);
        byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    public void send(String message) throws Exception {
        String hmac = computeHMAC(message);
        String signedMessage = message + "|" + hmac;
        byte[] data = signedMessage.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
        socket.send(packet);
    }

    public String receive() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        String[] parts = receivedData.split("\\|");
        if (parts.length != 2) return null;
        
        String message = parts[0];
        String receivedHMAC = parts[1];
        
        String computedHMAC = computeHMAC(message);
        if (!computedHMAC.equals(receivedHMAC)) {
            throw new SecurityException("Message authentication failed!");
        }
        return message;
    }
}
