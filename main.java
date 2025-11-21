package sistema_multiagente;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 * ESTRATÉGIA DE INICIALIZAÇÃO:
 * 
 * 1. CONFIGURAÇÃO DO AMBIENTE:
 *    - Inicia container principal na porta 2099
 *    - Ativa interface gráfica do JADE
 * 
 * 2. CRIAÇÃO DE VENDEDORES:
 *    - Dois vendedores com estoques diferentes
 *    - Cada um registra seu próprio estoque no DF
 *    - Formato: "Título:Quantidade:Preço:PreçoMínimo"
 * 
 * 3. ORDEM DE INICIALIZAÇÃO:
 *    - Vendedores criados primeiro (com delay para registro)
 *    - Gerente criado por último para garantir disponibilidade dos vendedores
 */

public class Main {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "2099");
        p.setParameter(Profile.GUI, "true");

        try {
            AgentContainer mainContainer = rt.createMainContainer(p);
            
            // Primeiro inicia os vendedores
            Object[] estoqueVendedor1 = {
                "Dom Casmurro:3:45.50:40.00",
                "Memórias Póstumas de Brás Cubas:2:60.00:55.00"
            };
            
            Object[] estoqueVendedor2 = {
                "Dom Casmurro:2:50.00:45.00",
                "Iracema:4:40.00:35.00"
            };
            
            mainContainer.createNewAgent("Vendedor1", "sistema_multiagente.AgenteVendedor", estoqueVendedor1).start();
            mainContainer.createNewAgent("Vendedor2", "sistema_multiagente.AgenteVendedor", estoqueVendedor2).start();
            
            // Aguarda 2 segundos para registro no DF
            Thread.sleep(2000);
            
            // Depois inicia o gerente
            mainContainer.createNewAgent("Gerente", "sistema_multiagente.AgenteGerente", null).start();
            
            System.out.println("Sistema iniciado com 2 vendedores e 1 gerente");
            
        } catch (Exception e) {
            System.err.println("Erro ao iniciar sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}