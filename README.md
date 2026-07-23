# Live On — RuneLite Clan Hub

Plugin e API de apoio para o clã **Live On**. O projeto reúne feed de drops e
pets, anúncios da staff, consulta de membros, collection log recente e rankings
mensais de XP/EHP/EHB no painel lateral do RuneLite.

O código foi dividido por responsabilidade para que cada área possa ser alterada
sem exigir conhecimento do projeto inteiro.

## O que já está implementado

- autorização em duas etapas: clan chat local + lista oficial no servidor;
- token individual de sessão, sem webhook ou segredo do Discord no plugin;
- captura de drops por NPC e eventos do Loot Tracker;
- detecção de pets e novas entradas do collection log;
- embeds de drops, pets e anúncios enviados pelo backend ao Discord;
- anúncios no chat do jogo e mensagem de login;
- painel lateral com início, drops, busca de membros, perfil e rankings;
- perfil no estilo compacto do WOM, com resumo e abas Skills/Bosses/Atividades/Log;
- ranking de XP, EHP e EHB do mês atual ou anterior;
- integração centralizada com Wise Old Man, com cache do roster;
- backend FastAPI + SQLite pronto para Docker;
- catálogos editáveis de bosses, ranks e regras especiais de itens.

## Organização

```text
src/main/java/com/liveon/
├── LiveOnPlugin.java          entrada e eventos do RuneLite
├── auth/                      validação local e sessão
├── api/                       cliente HTTP e objetos JSON
├── events/                    drops, pets e collection log
├── announcements/             polling e mensagens no chat
├── assets/                    carregamento dos catálogos
└── ui/                        painel e cartões da interface

src/main/resources/catalog/   bosses, ranks e regras de itens
server/app/                    API, SQLite, WOM e Discord
docs/                          decisões e guias de manutenção
```

## Rodar o plugin em desenvolvimento

Requisitos: JDK 11 ou superior. O código produzido é Java 11, conforme o Plugin
Hub.

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

No RuneLite de desenvolvimento:

1. Ative o plugin **Live On**.
2. Nas configurações, ative `Ativar integração Live On`.
3. Para desenvolvimento local, use `http://localhost:8080` no endereço da API.
4. Entre em uma conta que esteja no clan chat `Live On` e no roster do backend.

Para contas Jagex, siga o guia oficial do RuneLite:
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts

## Rodar o backend

```powershell
cd server
Copy-Item .env.example .env
docker compose up --build
```

Edite `.env` antes de usar. Em produção, configure obrigatoriamente:

- `LIVE_ON_TOKEN_SECRET` com 32+ caracteres aleatórios;
- `LIVE_ON_ACCESS_CODE_SHA256` com o hash do código da staff;
- `LIVE_ON_WOM_GROUP_ID` com o grupo oficial da Live On;
- `LIVE_ON_DISCORD_WEBHOOK` somente no servidor;
- `LIVE_ON_STAFF_RSNS` com os RSNs autorizados a publicar.

A documentação interativa fica em `http://localhost:8080/docs`.

## Hospedar o backend na Discloud

O `discloud.config` da raiz publica somente a API FastAPI da pasta `server`.
Na integração GitHub da Discloud, selecione este repositório e cadastre as
variáveis de produção na área **Environment Variables**:

- `LIVE_ON_ENV=production`;
- `LIVE_ON_DATABASE_PATH=data/live-on.db`;
- `LIVE_ON_TOKEN_SECRET` com 32+ caracteres aleatórios;
- `LIVE_ON_ACCESS_CODE_SHA256` com o hash do código distribuído à staff;
- `LIVE_ON_WOM_GROUP_ID` com o ID numérico do grupo Live On;
- `LIVE_ON_WOM_API_KEY`, se o grupo utilizar chave;
- `LIVE_ON_STAFF_RSNS` com os RSNs da staff separados por vírgula;
- `LIVE_ON_DISCORD_WEBHOOK` com o webhook privado;
- `LIVE_ON_LOGIN_MESSAGE` e `LIVE_ON_CORS_ORIGINS`, conforme desejado.

O subdomínio configurado é `live-on-clan-api.discloud.app`. Caso o ID já esteja
ocupado, altere somente `ID` no `discloud.config`. Depois do deploy, confirme
`https://SEU-ID.discloud.app/health` e use essa URL nas configurações do plugin.

Para upload manual, compacte o conteúdo da raiz do repositório, mantendo
`discloud.config`, `server/requirements.txt` e `server/app/` nos mesmos caminhos.
O `.discloudignore` evita enviar o código Java e os artefatos locais que não são
necessários para executar a API.

## Segurança da entrada no clã

Uma senha colocada no `.jar` poderia ser extraída. Por isso o plugin:

1. verifica no próprio RuneLite se o personagem está no clan chat `Live On`;
2. envia RSN, rank e código para o backend;
3. o backend confirma o RSN no grupo do Wise Old Man (ou roster local em dev);
4. somente então emite um token individual e temporário.

O webhook do Discord, a chave do WOM e os segredos ficam exclusivamente no
servidor. Veja [docs/security.md](docs/security.md).

## Fontes técnicas

- Arquitetura de interface: plugin Flux (`abristow3/Flux-Runelite-Plugin`).
- Captura de eventos: arquitetura do Dink (`pajlads/DinkPlugin`).
- Estrutura de build: `runelite/example-plugin`.
- EHP/EHB e rankings: API v2 do Wise Old Man.

O código deste repositório é original; as referências foram usadas para entender
os eventos e padrões públicos das APIs.

## Antes de publicar no Plugin Hub

- trocar `https://api.liveonclan.com` pelo domínio real e HTTPS;
- informar o ID oficial do grupo WOM;
- substituir o ícone provisório pelo brasão oficial, se desejado;
- revisar a política de privacidade e a warning do Plugin Hub;
- testar login, drop, pet, anúncios e logout manualmente no jogo;
- enviar o repositório ao `runelite/plugin-hub` com um commit fixo.

Licença BSD-2-Clause.
