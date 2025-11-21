package sistema_multiagente;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Scanner;

/**
 * ESTRATÉGIA DO GERENTE:
 * 
 * 1. INTERFACE COM USUÁRIO:
 *    - Menu para capturar:
 *      * Título do livro desejado
 *      * Quantidade necessária
 *      * Preço máximo disposto a pagar
 * 
 * 2. CRIAÇÃO DE COMPRADORES:
 *    - Para cada pedido, cria um novo agente comprador
 *    - Passa os argumentos necessários para a negociação
 * 
 * 3. CONTROLE DO SISTEMA:
 *    - Permite encerrar o sistema quando selecionada a opção de saída
 */

public class AgenteGerente extends Agent {
    protected void setup() {
        System.out.println("Gerente " + getAID().getName() + " pronto.");
        
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                System.out.println("\n=== Sistema de Compras ===");
                System.out.println("1. Comprar livro");
                System.out.println("2. Sair");
                System.out.print("Escolha: ");
                
                try {
                    Scanner scanner = new Scanner(System.in);
                    int escolha = scanner.nextInt();
                    scanner.nextLine();
                    
                    switch (escolha) {
                        case 1:
                            System.out.print("Livro: ");
                            String titulo = scanner.nextLine();
                            
                            System.out.print("Quantidade: ");
                            int quantidade = scanner.nextInt();
                            
                            System.out.print("Preço máximo: R$");
                            double precoMaximo = scanner.nextDouble();
                            
                            try {
                                Object[] args = {titulo, String.valueOf(quantidade), String.valueOf(precoMaximo)};
                                getContainerController().createNewAgent(
                                    "comprador-" + System.currentTimeMillis(), 
                                    "sistema_multiagente.AgenteComprador", args).start();
                            } catch (Exception e) {
                                System.err.println("Erro ao criar comprador: " + e.getMessage());
                            }
                            break;
                            
                        case 2:
                            System.out.println("Encerrando...");
                            doDelete();
                            break;
                            
                        default:
                            System.out.println("Opção inválida");
                    }
                } catch (Exception e) {
                    System.err.println("Erro na entrada: " + e.getMessage());
                }
            }
        });
    }
    
    protected void takeDown() {
        System.out.println("Gerente " + getAID().getName() + " encerrando.");
    }
}