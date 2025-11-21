# 🧠 Sistema Multiagente para Mercado de Livros Usados
**Compradores e vendedores negociando automaticamente com estratégias inteligentes**

Este projeto implementa um sistema multiagente para compra e venda de livros usados, simulando um mercado virtual onde agentes negociam preços, disponibilidade e condições de venda. Toda a interação é automática, baseada em **estratégias de negociação configuradas no sistema**.

Desenvolvido em **JADE** para a disciplina de Inteligência Artificial.

---

## 🧩 Arquitetura do Sistema

### 👔 Agente Gerente (Interface com o usuário)
- Recebe o item desejado, quantidade e preço máximo do comprador.
- Cria os agentes dinamicamente e inicia o processo de negociação.
- Consolida resultados e exibe o melhor acordo alcançado.

### 🧍‍♂️ Agente Comprador
- Busca vendedores disponíveis.
- Inicia negociações paralelas com múltiplos agentes.
- Aplica estratégia de negociação automática para tentar obter o menor preço possível.
- Seleciona a melhor oferta encontrada.

### 🧍‍♀️ Agente Vendedor
- Mantém catálogo de livros com preços, quantidades e limites de negociação.
- Responde às propostas do comprador.
- Ajusta preço de acordo com estratégias de **concessão gradual**.

### 🏗 Ambiente JADE
- Plataforma para execução, registro e comunicação assíncrona entre agentes.
- Permite negociações simultâneas usando mensagens **ACL**.

---

## 🤝 Estratégias de Negociação

### 📘 Estratégias do Comprador
- **Oferta Inicial Conservadora:** Começa oferecendo 60% do preço máximo informado pelo usuário.  
- **Aumento Gradual da Proposta:** Incrementa 10% da diferença entre a oferta atual e o preço máximo por rodada.  
- **Timeout Inteligente:** Cancela negociações que excedam 30 segundos.  
- **Seleção Final da Melhor Oferta:** Após negociar com todos os vendedores, escolhe a proposta mais vantajosa.

### 📗 Estratégias dos Vendedores
- **Margem de Negociação Variável:** Oferece concessões entre 5% e 20%, conforme o comportamento do comprador.  
- **Defesa do Preço Mínimo:** Nunca vende abaixo do valor mínimo configurado no estoque.  
- **Gerenciamento de Estoque:** Atualiza quantidades em tempo real após cada negociação concluída.  
- **Resposta Adaptativa:** Ajusta a contraoferta com base nas propostas recebidas.
