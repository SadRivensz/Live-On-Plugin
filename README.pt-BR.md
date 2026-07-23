# Live On — Central do Clã no RuneLite

[English](README.md) | [Português (Brasil)](README.pt-BR.md)

Live On é um plugin do RuneLite acompanhado por uma API, desenvolvido para o clã
**Live On** de Old School RuneScape. Ele reúne drops, pets, anúncios da staff,
perfis dos membros, progresso das contas e corridas mensais em um único painel
lateral compacto.

O código é organizado por responsabilidade e usa catálogos editáveis para os
recursos e metadados do jogo. Assim, a manutenção continua acessível quando o
RuneLite, Wise Old Man, RuneProfile ou Old School RuneScape adicionarem conteúdo.

## Funcionalidades

- Validação do acesso em duas etapas: roster local do clã no RuneLite e roster
  oficial validado pelo servidor.
- Sessões temporárias por membro; webhook do Discord e chaves de serviços nunca
  são incluídos no JAR.
- Captura de loot de NPCs e eventos do Loot Tracker.
- Detecção de pets e novas entradas do Collection Log.
- Screenshots dos drops com visualização ampliada dentro do RuneLite.
- Screenshots clicáveis, itens detalhados e rodapé do clã nos embeds do Discord.
- Item desejado por membro, com parabéns automáticos e o número de dias da busca
  quando o item é obtido.
- Anúncios da staff no chat e mensagem de entrada exibida somente quando o
  próprio jogador local faz login.
- Painel compacto com início, drops recentes, busca de membros e ranking mensal.
- Perfis do Wise Old Man com skills, KCs de bosses, atividades, EHP e EHB.
- Popup do RuneProfile com conclusão por boss, raid, clue e atividade, separando
  itens obtidos e faltantes.
- Ícones nativos do Hiscore do RuneLite e ícones oficiais dos ranks do clã.
- Avatar do Discord opcional para membros que vincularam a conta no site da
  Live On.
- Grades responsivas e paginação feitas para a largura reduzida do painel.
- Rankings de XP, EHP e EHB do mês atual e do mês anterior.
- Cache compartilhado e união de requisições ao Wise Old Man e RuneProfile.
- Serviço FastAPI e SQLite pronto para desenvolvimento local, Docker ou Discloud.

## Arquitetura

```text
Plugin do RuneLite
    │ eventos autenticados e consultas de perfil
    ▼
API FastAPI da Live On
    ├── Wise Old Man: roster, snapshots, XP, EHP e EHB
    ├── RuneProfile: detalhes opcionais do Collection Log
    ├── Site da Live On: avatar público opcional do Discord
    ├── SQLite: sessões, drops, objetivos e anúncios
    └── Discord: envio do webhook somente pelo servidor
```

```text
src/main/java/com/liveon/
├── LiveOnPlugin.java          ciclo de vida e eventos do RuneLite
├── auth/                      validação local e sessões
├── api/                       cliente HTTP e modelos de transporte
├── events/                    drops, pets e Collection Log
├── announcements/             consultas e mensagens no jogo
├── assets/                    carregamento de catálogos e ícones
└── ui/                        painel, cartões e popups

src/main/resources/catalog/    bosses, ranks e regras de itens editáveis
server/app/                    FastAPI, SQLite e integrações
server/tests/                  testes unitários do backend
docs/                          API, manutenção e segurança
```

## Desenvolvimento do plugin

### Requisitos

- JDK 11 ou superior. O bytecode gerado usa Java 11 para compatibilidade com o
  Plugin Hub.
- Uma instância da API Live On para as funções autenticadas.

### Compilar e executar

```powershell
.\gradlew.bat test
.\gradlew.bat run
```

No RuneLite de desenvolvimento:

1. Ative **Live On**.
2. Abra as configurações e marque **Ativar integração Live On**.
3. Para testes locais, use `http://127.0.0.1:8080` no endereço da API.
4. Entre com uma conta que esteja no clan chat `Live On` e no roster da API.

Quem utiliza Jagex Account pode seguir o
[guia oficial de desenvolvimento do RuneLite](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Executar a API localmente

Python 3.12 é recomendado.

```powershell
cd server
Copy-Item .env.example .env
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --host 127.0.0.1 --port 8080 --env-file .env
```

Edite `server/.env` antes de iniciar. Verifique o serviço em:

- Saúde da API: `http://127.0.0.1:8080/health`
- Documentação interativa: `http://127.0.0.1:8080/docs`

Docker é uma alternativa opcional:

```powershell
cd server
Copy-Item .env.example .env
docker compose up --build
```

## Configuração do servidor

O modelo completo está em [`server/.env.example`](server/.env.example).
As variáveis principais são:

| Variável | Finalidade |
| --- | --- |
| `LIVE_ON_ENV` | `development` ou `production` |
| `LIVE_ON_DATABASE_PATH` | Caminho do banco SQLite |
| `LIVE_ON_SCREENSHOT_DIRECTORY` | Pasta das screenshots armazenadas |
| `LIVE_ON_PUBLIC_BASE_URL` | Origem pública da API usada nos links das imagens |
| `LIVE_ON_TOKEN_SECRET` | Segredo aleatório com pelo menos 32 caracteres |
| `LIVE_ON_ACCESS_CODE_SHA256` | Hash SHA-256 do código privado do clã |
| `LIVE_ON_WOM_GROUP_ID` | ID do grupo oficial no Wise Old Man |
| `LIVE_ON_WOM_API_KEY` | Chave opcional do Wise Old Man |
| `LIVE_ON_RUNEPROFILE_API_KEY` | Chave opcional para ampliar o limite do RuneProfile |
| `LIVE_ON_BOOTSTRAP_MEMBERS` | Roster alternativo somente para desenvolvimento |
| `LIVE_ON_STAFF_RSNS` | RSNs da staff separados por vírgula |
| `LIVE_ON_DISCORD_WEBHOOK` | URL privada do webhook do Discord |
| `LIVE_ON_MEMBER_SITE_BASE_URL` | Origem opcional dos perfis públicos da Live On |
| `LIVE_ON_LOGIN_MESSAGE` | Mensagem mostrada ao jogador local depois do login |
| `LIVE_ON_CORS_ORIGINS` | Origens web separadas por vírgula |

Gere o hash do código de acesso no PowerShell:

```powershell
([BitConverter]::ToString(
  [Security.Cryptography.SHA256]::HashData(
    [Text.Encoding]::UTF8.GetBytes('substitua-pelo-seu-codigo')
  )
)).Replace('-', '').ToLower()
```

Não coloque o código original na configuração do servidor. O plugin recebe o
código do membro e o servidor armazena somente o hash SHA-256.

## Hospedar a API na Discloud

Este repositório já possui um `discloud.config` pronto para produção na raiz.
Ele inicia somente a API Python da pasta `server/`; o build do RuneLite não é
necessário no servidor.

### Opção A — integração com GitHub

1. Faça fork ou envie este repositório para a sua conta do GitHub.
2. No painel da Discloud, abra **GitHub Integration**, autorize a mesma conta que
   possui o repositório e conceda acesso a ele.
3. Selecione **Upload → GitHub**, escolha o repositório e a branch e confirme que
   a Discloud encontrou o `discloud.config` da raiz.
4. Em **Environment Variables**, cadastre os valores de produção listados abaixo.
5. Inicie o deploy e acompanhe os logs de build e inicialização até a aplicação
   ficar online.

### Opção B — upload por ZIP

1. Crie um ZIP cujo primeiro nível contenha `discloud.config`,
   `server/requirements.txt` e `server/app/`. Não coloque tudo dentro de uma
   pasta pai adicional.
2. Exclua `.git`, `.gradle`, `build`, ambientes virtuais, `__pycache__`, bancos
   locais e o `.env` real.
3. No painel da Discloud, selecione **Upload** e envie o ZIP.
4. Cadastre as variáveis de produção e inicie a aplicação.

O ZIP de release gerado por este projeto já está organizado para esta opção.

### Valores obrigatórios em produção

Configure pelo menos:

```dotenv
LIVE_ON_ENV=production
LIVE_ON_DATABASE_PATH=data/live-on.db
LIVE_ON_SCREENSHOT_DIRECTORY=data/screenshots
LIVE_ON_PUBLIC_BASE_URL=https://SEU-ID.discloud.app
LIVE_ON_TOKEN_SECRET=troque-por-um-segredo-aleatorio-longo
LIVE_ON_ACCESS_CODE_SHA256=troque-pelo-hash-do-codigo
LIVE_ON_WOM_GROUP_ID=troque-pelo-id-do-grupo
LIVE_ON_STAFF_RSNS=Staff Um,Staff Dois
LIVE_ON_DISCORD_WEBHOOK=troque-pelo-webhook-privado
LIVE_ON_MEMBER_SITE_BASE_URL=https://seu-site-de-membros.example
LIVE_ON_LOGIN_MESSAGE="Bem-vindo à Live On! Confira os anúncios no painel lateral."
LIVE_ON_CORS_ORIGINS=https://seu-site-publico.example
LIVE_ON_TOKEN_LIFETIME_SECONDS=43200
```

Variáveis opcionais, como `LIVE_ON_WOM_API_KEY` e
`LIVE_ON_RUNEPROFILE_API_KEY`, podem ficar vazias. Use
`LIVE_ON_BOOTSTRAP_MEMBERS` somente como alternativa para desenvolvimento, não
como roster oficial de produção.

### Verificar o deploy

1. Abra `https://SEU-ID.discloud.app/health` e confirme a resposta de sucesso.
2. Abra `https://SEU-ID.discloud.app/docs` se a documentação puder ficar pública.
3. Configure o endereço da API no plugin como
   `https://SEU-ID.discloud.app`, sem barra no final.
4. Teste a autenticação, um anúncio da staff, uma screenshot de drop e um ranking.
5. Faça backup de `data/live-on.db` e `data/screenshots` antes de remover a
   aplicação ou realizar um redeploy destrutivo.

Nunca envie ao GitHub o `.env` de produção, webhook do Discord, chaves de API,
código de acesso ou segredo dos tokens. Mantenha tudo na configuração de
variáveis de ambiente da Discloud.

## Acesso ao clã e privacidade

Uma senha embutida em um JAR pode ser extraída. Por isso a Live On faz duas
verificações:

1. O RuneLite confirma que o personagem local está no clan chat `Live On`.
2. A API valida o RSN no roster oficial do Wise Old Man ou no fallback de
   desenvolvimento configurado explicitamente.
3. Somente depois disso a API emite um token individual e temporário.

Webhook do Discord, chave do Wise Old Man, chave do RuneProfile e segredo de
assinatura permanecem no servidor. Eventos do jogador e screenshots são enviados
apenas quando a integração está ativa e a conta está autorizada. Consulte
[`docs/security.md`](docs/security.md) para o modelo de ameaças detalhado.

## Testes e build de release

```powershell
.\gradlew.bat clean test shadowJar --no-build-cache

cd server
python -m unittest discover -s tests -v
```

O JAR completo do plugin é salvo em `build/libs/`.

Antes de solicitar a revisão no Plugin Hub:

- confirme que o endpoint `/health` de produção responde;
- revise o aviso do Plugin Hub e a declaração de privacidade;
- teste manualmente login, logout, drop, pet, objetivo, anúncio e screenshot;
- teste os perfis e todos os modos do ranking mensal;
- envie um hash de commit imutável ao `runelite/plugin-hub`.

## Referências técnicas

- Inspiração da interface: [Flux RuneLite Plugin](https://github.com/abristow3/Flux-Runelite-Plugin)
- Referência de eventos: [Dink](https://github.com/pajlads/DinkPlugin)
- Estrutura de build: [RuneLite example plugin](https://github.com/runelite/example-plugin)
- Contas e eficiência: [Wise Old Man API](https://docs.wiseoldman.net/)
- Collection Log: [RuneProfile API](https://api.runeprofile.com/v1/docs)
- Hospedagem: [documentação da Discloud](https://docs.discloud.com/how-to-host-using/dashboard)

A implementação deste repositório é original. Os projetos acima foram usados
como referências públicas para eventos do RuneLite, convenções de interface e
comportamento das APIs.

## Licença

[BSD 2-Clause](LICENSE)
