# Contribuindo

[English](CONTRIBUTING.md) | [Português (Brasil)](CONTRIBUTING.pt-BR.md)

1. Uma classe deve ter uma responsabilidade clara.
2. Eventos do RuneLite não fazem I/O bloqueante.
3. HTTP passa por `LiveOnApiClient`; SQL passa por `Database`.
4. Não adicione segredos, webhooks ou `.env` ao Git.
5. Use constantes `gameval` quando precisar de IDs fixos do jogo.
6. Adicione um teste para regras de autorização ou transformação de dados.
7. Rode `gradlew test` e os testes do backend antes de abrir um pull request.

Formatação e nomes devem priorizar leitura. Comentários explicam decisões, não
repetem o que a linha de código já diz.
