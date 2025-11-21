package sistema_multiagente;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

/**
 * ESTRATÉGIA DO COMPRADOR:
 * 
 * 1. BUSCA POR VENDEDORES:
 *    - Consulta o DF (Directory Facilitator) para encontrar todos os vendedores de "venda-livros"
 *    - Envia CFP (Call For Proposal) para todos os vendedores encontrados
 * 
 * 2. SELEÇÃO DE OFERTAS:
 *    - Coleta todas as propostas (PROPOSE) recebidas
 *    - Seleciona a oferta com MENOR PREÇO
 * 
 * 3. NEGOCIAÇÃO:
 *    - Oferece inicialmente 70% do preço cotado pelo vendedor
 *    - A cada rodada, aumenta 15% da diferença entre sua oferta e a do vendedor
 *    - Critérios de aceitação:
 *      * Se o preço do vendedor ≤ preço máximo do usuário
 *      * Se a oferta estiver dentro de 15% da última proposta
 *      * Máximo de 5 rodadas de negociação
 *    - Nunca ultrapassa o preço máximo definido pelo usuário
 */

public class AgenteComprador extends Agent {
    private String livroDesejado;
    private int quantidadeDesejada;
    private double precoMaximo;
    private Map<AID, Double> ofertas = new HashMap<>();
    private AID melhorVendedor;
    
    protected void setup() {
        System.out.println("Comprador " + getAID().getName() + " pronto.");
        
        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            livroDesejado = (String) args[0];
            quantidadeDesejada = Integer.parseInt((String) args[1]);
            precoMaximo = Double.parseDouble((String) args[2]);
            
            addBehaviour(new TickerBehaviour(this, 10000) {
                protected void onTick() {
                    System.out.println("Buscando: " + livroDesejado);
                    buscarVendedores();
                }
            });
        } else {
            System.out.println("Argumentos inválidos");
            doDelete();
        }
    }
    
    private void buscarVendedores() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("venda-livros");
        template.addServices(sd);
        
        try {
            // Tenta 3 vezes com intervalo de 1 segundo
            int tentativas = 0;
            DFAgentDescription[] result = new DFAgentDescription[0];
            
            while (tentativas < 3 && result.length == 0) {
                result = DFService.search(this, template);
                System.out.println("Tentativa " + (tentativas+1) + ": " + result.length + " vendedores");
                if (result.length == 0) {
                    Thread.sleep(1000);
                }
                tentativas++;
            }
            
            if (result.length > 0) {
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (DFAgentDescription seller : result) {
                    cfp.addReceiver(seller.getName());
                }
                cfp.setContent(livroDesejado + ":" + quantidadeDesejada);
                cfp.setConversationId("negociacao-livros");
                cfp.setReplyWith("cfp" + System.currentTimeMillis());
                send(cfp);
                
                addBehaviour(new ReceberOfertasBehaviour(this, 5000));
            } else {
                System.out.println("Nenhum vendedor encontrado para " + livroDesejado);
            }
        } catch (Exception e) {
            System.err.println("Erro na busca por vendedores");
            e.printStackTrace();
        }
    }
    
    private class ReceberOfertasBehaviour extends TickerBehaviour {
        public ReceberOfertasBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        protected void onTick() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchConversationId("negociacao-livros"),
                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            
            ACLMessage msg = receive(mt);
            while (msg != null) {
                String[] parts = msg.getContent().split(":");
                double preco = Double.parseDouble(parts[0]);
                int quantidade = Integer.parseInt(parts[1]);
                
                if (quantidade >= quantidadeDesejada) {
                    ofertas.put(msg.getSender(), preco);
                    System.out.println("Oferta de " + msg.getSender().getLocalName() + ": R$" + preco);
                }
                msg = receive(mt);
            }
            
            if (!ofertas.isEmpty() && melhorVendedor == null) {
                selecionarMelhorOferta();
            }
        }
        
        private void selecionarMelhorOferta() {
            melhorVendedor = Collections.min(ofertas.entrySet(), 
                Comparator.comparingDouble(Map.Entry::getValue)).getKey();
            double melhorPreco = ofertas.get(melhorVendedor);
            
            System.out.println("Melhor oferta: " + melhorVendedor.getLocalName() + " - R$" + melhorPreco);
            
            ACLMessage proposta = new ACLMessage(ACLMessage.PROPOSE);
            proposta.addReceiver(melhorVendedor);
            proposta.setContent((melhorPreco * 0.7) + ":" + quantidadeDesejada);
            proposta.setConversationId("negociacao-livros");
            send(proposta);
            
            addBehaviour(new NegociarBehaviour(melhorVendedor, melhorPreco));
        }
    }
    
 // Estratégia de barganha:
 // 1. Começa oferecendo 70% do preço inicial do vendedor
 // 2. A cada contraproposta, aumenta a oferta em 15% da diferença
//     Ex: Se vendedor pede R$100 e oferecemos R$70:
//         Diferença = R$30 → 15% = R$4.50 → Nova oferta = R$74.50
 // 3. Nunca ultrapassa o preço máximo do usuário
 // 4. Aceita se: oferta ≤ preço máximo E (rodadas ≥5 ou oferta ≤ última oferta +15%)
    
    private class NegociarBehaviour extends Behaviour {
        private final AID vendedor;
        private double precoAtual;
        private boolean negociacaoConcluida = false;
        private int rodada = 0;
        
        public NegociarBehaviour(AID vendedor, double precoInicial) {
            this.vendedor = vendedor;
            this.precoAtual = precoInicial * 0.7;
        }
        
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchSender(vendedor),
                MessageTemplate.MatchConversationId("negociacao-livros"));
            
            ACLMessage msg = receive(mt);
            if (msg != null) {
                rodada++;
                
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("Compra aprovada com " + vendedor.getLocalName() + " por R$" + precoAtual);
                    negociacaoConcluida = true;
                } 
                else if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    String[] parts = msg.getContent().split(":");
                    double contraProposta = Double.parseDouble(parts[0]);
                    
                    if (contraProposta <= precoMaximo) {
                        if (rodada >= 5 || contraProposta <= (precoAtual * 1.15)) {
                            ACLMessage accept = msg.createReply();
                            accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            send(accept);
                            negociacaoConcluida = true;
                        } else {
                            precoAtual += (contraProposta - precoAtual) * 0.15;
                            precoAtual = Math.min(precoAtual, precoMaximo);
                            
                            ACLMessage counter = msg.createReply();
                            counter.setPerformative(ACLMessage.PROPOSE);
                            counter.setContent(precoAtual + ":" + quantidadeDesejada);
                            send(counter);
                        }
                    } else {
                        ACLMessage reject = msg.createReply();
                        reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        send(reject);
                        negociacaoConcluida = true;
                    }
                }
            } else {
                block();
            }
        }
        
        public boolean done() {
            return negociacaoConcluida;
        }
    }
    
    protected void takeDown() {
        System.out.println("Comprador " + getAID().getName() + " encerrando.");
    }
}