package consensus;

import communication.AuthenticatedPerfectLink;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LeaderNode extends ConsensusNode {

	private final ConditionalCollect conditionalCollect;
	private final Set<Integer> followerNodes; // Conjunto de processos seguidores

	public LeaderNode(int nodeId, AuthenticatedPerfectLink apl, Set<Integer> followers) throws Exception {
		super(nodeId, apl);
		this.conditionalCollect = new ConditionalCollect(nodeId, apl);
		this.followerNodes = followers; // Lista de processos que não são líderes
	}

// 🔹 Enviar READ para os outros processos
	public void sendReadRequest() {
		String readMessage = "READ"; // Mensagem READ enviada pelo líder

		System.out.println("Líder enviando READ para os seguidores...");

		for (int followerId : followerNodes) {
			apls.get(followerId).send(readMessage, followerId); // Enviar READ usando o link confiável
			System.out.println("READ enviado para " + followerId);
		}
	}

// 🔹 Receber o estado (valor e timestamp) dos seguidores
	public void receiveReadResponse(int senderId, Object[] tsvalue, Map<Integer, String> writeset) {

// Envia evento para coleta condicional
		conditionalCollect.insertMessage(senderId, tsvalue, writeset);
	}

// 🔹 Enviar o valor final decidido para os seguidores
	private void sendDecidedValueToFollowers(String value) {
		for (int followerId : followerNodes) {
			apls.send("DECIDED_VALUE|" + value, followerId); // Enviar o valor decidido
			System.out.println("Valor decidido enviado para " + followerId + ": " + value);
		}
	}
}