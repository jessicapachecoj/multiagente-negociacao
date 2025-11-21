package sistema_multiagente;

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
 * ESTRATÉGIA DO VENDEDOR:
 * 
 * 1. REGISTRO:
 *    - Registra no DF como serviço do tipo "venda-livros"
 *    - Mantém um estoque com: quantidade, preço inicial e preço mínimo
 * 
 * 2. RESPOSTA A PEDIDOS:
 *    - Para CFP (Call For Proposal), verifica:
 *      * Se tem o livro em estoque 
 *      * Se tem quantidade suficiente 
 * 
 * 3. NEGOCIAÇÃO:
 *    - Reduz 20% da diferença entre seu preço e a oferta do comprador
 *    - Nunca abaixa abaixo do preço mínimo
 *    - Critérios de aceitação:
 *      * Se atingir o preço mínimo
 *      * Após 5 rodadas de negociação
 *    - Atualiza estoque após venda concluída
 */

public class AgenteVendedor extends Agent {
    private Map<String, Object[]> estoque;

    protected void setup() {
        System.out.println("Vendedor " + getAID().getName() + " iniciando...");
        
        // Registro no DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("venda-livros");
        sd.setName(getLocalName() + "-livros-usados");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registrado no DF");
        } catch (FIPAException fe) {
            System.err.println("Falha no registro do DF");
            fe.printStackTrace();
            doDelete();
            return;
        }

        // Inicializa estoque
        estoque = new HashMap<>();
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            System.out.println("Estoque inicial de " + getLocalName() + ":");
            for (Object arg : args) {
                String[] dados = ((String) arg).split(":");
                String titulo = dados[0].trim();
                int quantidade = Integer.parseInt(dados[1].trim());
                double preco = Double.parseDouble(dados[2].trim());
                double precoMinimo = Double.parseDouble(dados[3].trim());
                estoque.put(titulo, new Object[]{quantidade, preco, precoMinimo});
                System.out.println("- " + titulo + ": " + quantidade + "un, R$" + preco + " (min R$" + precoMinimo + ")");
            }
        }

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("negociacao-livros"));
                
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    processarPedido(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void processarPedido(ACLMessage msg) {
        String[] conteudo = msg.getContent().split(":");
        String titulo = conteudo[0].trim();
        int quantidadePedido = Integer.parseInt(conteudo[1].trim());
        
        ACLMessage resposta = msg.createReply();
        
        if (estoque.containsKey(titulo)) {
            Object[] livro = estoque.get(titulo);
            int quantidadeEstoque = (int) livro[0];
            double preco = (double) livro[1];
            double precoMinimo = (double) livro[2];
            
            if (quantidadeEstoque >= quantidadePedido) {
                resposta.setPerformative(ACLMessage.PROPOSE);
                resposta.setContent(preco + ":" + quantidadeEstoque);
            } else {
                resposta.setPerformative(ACLMessage.REFUSE);
                resposta.setContent("estoque-insuficiente");
            }
        } else {
            resposta.setPerformative(ACLMessage.REFUSE);
            resposta.setContent("livro-inexistente");
        }
        send(resposta);
        
        if (resposta.getPerformative() == ACLMessage.PROPOSE) {
            addBehaviour(new ComportamentoNegociacao(
                msg.getSender(), titulo, quantidadePedido, 
                (double) estoque.get(titulo)[1], 
                (double) estoque.get(titulo)[2]));
        }
    }
    
 // Estratégia de desconto progressivo:
 // 1. Reduz 20% da diferença entre seu preço e a oferta do comprador
//     Ex: Se pede R$100 e comprador oferece R$70:
//         Diferença = R$30 → 20% = R$6 → Nova oferta = R$94
 // 2. Nunca abaixa abaixo do preço mínimo
 // 3. Na 5ª rodada ou ao atingir o mínimo, envia última oferta

    private class ComportamentoNegociacao extends Behaviour {
        private final AID comprador;
        private final String titulo;
        private final int quantidade;
        private double precoAtual;
        private final double precoMinimo;
        private int rodadas = 0;
        private boolean negociacaoConcluida = false;
        
        public ComportamentoNegociacao(AID comprador, String titulo, int quantidade, 
                                     double precoInicial, double precoMinimo) {
            this.comprador = comprador;
            this.titulo = titulo;
            this.quantidade = quantidade;
            this.precoAtual = precoInicial;
            this.precoMinimo = precoMinimo;
        }
        
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchSender(comprador),
                MessageTemplate.MatchConversationId("negociacao-livros"));
            
            ACLMessage msg = receive(mt);
            if (msg != null) {
                rodadas++;
                
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    Object[] livro = estoque.get(titulo);
                    int novoEstoque = (int) livro[0] - quantidade;
                    
                    if (novoEstoque >= 0) {
                        estoque.put(titulo, new Object[]{novoEstoque, precoAtual, precoMinimo});
                        ACLMessage confirmacao = msg.createReply();
                        confirmacao.setPerformative(ACLMessage.CONFIRM);
                        confirmacao.setContent("venda-concluida:" + precoAtual);
                        send(confirmacao);
                        System.out.println(getLocalName() + " vendeu " + quantidade + "x " + titulo + " por R$" + precoAtual);
                    } else {
                        ACLMessage cancel = msg.createReply();
                        cancel.setPerformative(ACLMessage.CANCEL);
                        cancel.setContent("estoque-esgotado");
                        send(cancel);
                    }
                    negociacaoConcluida = true;
                } 
                else if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    double ofertaComprador = Double.parseDouble(msg.getContent().split(":")[0]);
                    double novaOferta = precoAtual - (precoAtual - ofertaComprador) * 0.2;
                    novaOferta = Math.max(novaOferta, precoMinimo);
                    
                    ACLMessage resposta = msg.createReply();
                    if (rodadas >= 5 || novaOferta == precoMinimo) {
                        resposta.setPerformative(ACLMessage.PROPOSE);
                        resposta.setContent(precoMinimo + ":" + quantidade);
                        resposta.addUserDefinedParameter("ultima-oferta", "true");
                    } else {
                        resposta.setPerformative(ACLMessage.PROPOSE);
                        resposta.setContent(novaOferta + ":" + quantidade);
                        precoAtual = novaOferta;
                    }
                    send(resposta);
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
        try {
            DFService.deregister(this);
            System.out.println(getLocalName() + " encerrando e saindo do DF");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}