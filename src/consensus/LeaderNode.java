package consensus;
import config.MembershipConfig.NodeInfo;

import communication.NetworkHandler;
import consensus.ConsensusMessage;
import config.MembershipConfig;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class LeaderNode extends ConsensusNode {
	private final Map<Integer, Map<String, Integer>> voteCounts; // Armazena contagem de votos por epoch

	public LeaderNode(int nodeId, int port) throws Exception {
		super(nodeId, port);
		this.voteCounts = new HashMap<>();
	}

	/**
	 * Envia uma proposta para os seguidores.
	 */
	public void sendProposal(int epoch, String value) throws Exception {
		ConsensusMessage proposal = new ConsensusMessage(ConsensusMessage.MessageType.PROPOSE, epoch, value, getPrivateKey());

		// Envia a proposta para todos os nós do sistema
		for (NodeInfo node : getMembershipConfig().getAllNodeInfo().values()) {
			if (node.getId() != getNodeId()) {
				InetAddress address = node.getAddress();
				int port = node.getPort();
				getNetworkHandler().sendMessage(proposal.toString(), address, port);
				System.out.println("Leader " + getNodeId() + " sent proposal to node " + node.getId() + " for epoch " + epoch + ": " + value);
			}
		}
	}

	/**
	 * Recebe e processa um voto de um nó seguidor.
	 */
	public void receiveVote(int epoch, String value) {
		voteCounts.putIfAbsent(epoch, new HashMap<>());
		voteCounts.get(epoch).put(value, voteCounts.get(epoch).getOrDefault(value, 0) + 1);

		System.out.println("Leader " + getNodeId() + " received vote for value: " + value + " in epoch " + epoch);

		// Verifica se atingiu maioria (2/3 dos nós)
		int majority = (getMembershipConfig().getNodeCount() * 2) / 3;
		if (voteCounts.get(epoch).get(value) >= majority) {
			decideValue(epoch, value);
		}
	}

	/**
	 * Envia a decisão final para todos os nós.
	 */
	public void decideValue(int epoch, String value) {
		try {
			// Tenta criar a mensagem de decisão
			ConsensusMessage decision = new ConsensusMessage(ConsensusMessage.MessageType.DECIDE, epoch, value, getPrivateKey());

			// Enviar a decisão para todos os nós
			for (NodeInfo node : getMembershipConfig().getAllNodeInfo().values()) {
				InetAddress address = node.getAddress();  // Pega o endereço do nó
				int port = node.getPort();
				getNetworkHandler().sendMessage(decision.toString(), address, port);  // Envia a mensagem
			}

			System.out.println("Leader " + getNodeId() + " decided on value: " + value + " for epoch " + epoch);
		} catch (Exception e) {
			// Se houver erro ao criar a mensagem de decisão, imprima o erro
			System.err.println("Error creating decision message: " + e.getMessage());
			e.printStackTrace();  // Para depuração, imprime o stack trace
		}
	}
}
