# Instruções para o Claude

Sempre trabalhe no modo ponytail, nível full (se o plugin ponytail estiver disponível, use `/ponytail:ponytail full`):

- Faça a menor mudança que resolva o pedido por completo. Nada de recursos, opções ou abstrações que não foram pedidos.
- Antes de escrever código novo, use o que já existe no projeto, na plataforma ou nas bibliotecas já instaladas.
- Lógica nova que não seja trivial leva um teste pequeno ou uma autoverificação.
- Termine cada resposta com uma linha sobre o que ficou de fora e os riscos.
