package nodes;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;

public class LeaderNode extends ConsensusNode {

    public LeaderNode(DatagramSocket leaderSocket, InetAddress leadersAddress) throws NoSuchAlgorithmException {
        super(leaderSocket, leadersAddress);
    }

}
