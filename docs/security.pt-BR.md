# Segurança e privacidade

[English](security.md) | [Português (Brasil)](security.pt-BR.md)

## O que o plugin envia

Somente após o membro ativar a integração:

- RSN e rank usados na autorização;
- origem, itens e valor dos drops que passam do limite configurado;
- mensagens locais que identificam pet ou nova entrada do Collection Log;
- uma screenshot do drop quando a captura está ativa.

O endereço IP será visível ao servidor como em qualquer requisição HTTPS. Por
isso a opção é desativada por padrão e possui o aviso exigido pelo Plugin Hub.

## O que nunca deve entrar no plugin

- webhook do Discord;
- segredo HMAC do backend;
- chaves de API do Wise Old Man ou RuneProfile;
- código de acesso em texto fixo no repositório;
- credenciais da conta RuneScape ou Jagex.

## Limitações conhecidas

- O clan chat local confirma contexto, mas o backend deve continuar sendo a
  autoridade final.
- Um código compartilhado pode vazar; a validação por RSN e tokens temporários
  reduz o impacto. No futuro, pode-se trocar o código por vínculo Discord/RSN.
- O Collection Log histórico não é público no Wise Old Man. O projeto armazena
  entradas novas observadas no cliente; a importação histórica exige
  consentimento e outra fonte.
- Use HTTPS obrigatório em produção e limite requisições no proxy reverso.
