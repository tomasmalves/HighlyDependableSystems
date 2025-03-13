package consensus;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import communication.NetworkHandler;
import config.MembershipConfig;
import java.security.*;

public class ConsensusNode {
    private final int nodeId;
    private final NetworkHandler networkHandler;
    private final Map<Integer, String> proposedValues;
    private final Map<Integer, String> votes; // Armazena os votos dos nós
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final MembershipConfig membershipConfig;

    public ConsensusNode(int nodeId, int port) throws Exception {
        this.nodeId = nodeId;
        this.networkHandler = new NetworkHandler(port);
        this.proposedValues = new HashMap<>();
        this.votes = new HashMap<>();

        // Gerar par de chaves para assinar/verificar mensagens
        KeyPair keyPair = generateKeyPair();
        this.publicKey = keyPair.getPublic();
        this.privateKey = keyPair.getPrivate();

        // Inicializa a configuração de membros
        this.membershipConfig = new MembershipConfig(nodeId);

        System.out.println("ConsensusNode inicializado com ID: " + nodeId);
        System.out.println("Total de nós no sistema: " + membershipConfig.getNodeCount());
        System.out.println("Líder atual: " + membershipConfig.getLeaderInfo().getId());

        if (membershipConfig.isLeader()) {
            System.out.println("Este nó é o LÍDER");
        } else {
            System.out.println("Este nó é um SEGUIDOR");
        }
    }

    public int getNodeId() {
        return nodeId;
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Propor um valor no consenso
     */
    public void proposeValue(int epoch, String value) {
        proposedValues.put(epoch, value);
        System.out.println("Nó " + nodeId + " propôs valor para época " + epoch + ": " + value);
    }

    /**
     * Decidir sobre um valor para uma determinada época
     */
    public void decideValue(int epoch, String value) {
        if (value != null) {
            System.out.println("Nó " + nodeId + " decidiu pelo valor: " + value + " na época " + epoch);
        } else {
            System.out.println("Nó " + nodeId + " não tem valor proposto para época " + epoch);
        }
    }

    /**
     * Retorna o valor proposto para um determinado epoch
     */
    public String getProposedValue(int epoch) {
        return proposedValues.getOrDefault(epoch, null);
    }

    /**
     * Enviar um voto para o líder
     */
    public void sendVote(int epoch, String value, InetAddress leaderAddress, int leaderPort) throws Exception {
        ConsensusMessage voteMessage = new ConsensusMessage(ConsensusMessage.MessageType.VOTE, epoch, value, privateKey);
        networkHandler.sendMessage(voteMessage.toString(), leaderAddress, leaderPort);
        System.out.println("Nó " + nodeId + " votou no valor: " + value + " na época " + epoch);
    }

    /**
     * Receber um voto de outro nó e armazená-lo
     */
    public void receiveVote(int epoch, String value) {
        votes.put(epoch, value);
        System.out.println("Nó " + nodeId + " recebeu voto para valor: " + value + " na época " + epoch);
    }

    /**
     * Retorna a configuração de membros
     */
    public MembershipConfig getMembershipConfig() {
        return membershipConfig;
    }

    /**
     * Retorna o manipulador de rede
     */
    public NetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    /**
     * Iniciar o protocolo de consenso
     */
    public void start() {
        System.out.println("Nó " + nodeId + " iniciando protocolo de consenso...");
        // Implementação do protocolo será adicionada aqui
    }
}
