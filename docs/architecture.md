# Arquitetura

```mermaid
flowchart LR
    Game["Eventos do jogo"] --> Plugin["Plugin RuneLite"]
    Clan["Clan chat Live On"] --> Auth["Validação local"]
    Auth --> Plugin
    Plugin -->|"HTTPS + token"| API["API Live On"]
    API --> DB[("SQLite / banco futuro")]
    API --> WOM["Wise Old Man v2"]
    API --> Discord["Webhook Discord"]
    Site["Página da Live On"] -->|"mesma API"| API
    API --> Announcements["Anúncios e mensagem de login"]
    Announcements --> Plugin
```

## Regra principal

O RuneLite captura apenas eventos do personagem local. Consolidação de dados,
permissões de staff, WOM e Discord ficam no backend. Isso evita distribuir
segredos e deixa o site consumir a mesma fonte usada pelo plugin.

## Cliente RuneLite

- `LiveOnPlugin`: registra/desregistra componentes e encaminha eventos.
- `ClanAccessManager`: lê a sessão local do RuneLite e pede autorização.
- `LiveOnApiClient`: único lugar que conhece URLs, JSON e headers.
- `events/*`: transforma eventos em objetos pequenos; não desenha interface.
- `AnnouncementService`: consulta anúncios fora da client thread.
- `ui/*`: somente renderiza objetos vindos da API.

## Backend

- `main.py`: rotas e regras de autorização.
- `database.py`: SQL e persistência isolados.
- `wom.py`: cache e adaptação da API Wise Old Man.
- `discord.py`: formatação dos embeds.
- `security.py`: hash do código e tokens HMAC temporários.

SQLite atende a primeira fase. Para Postgres, mantenha as mesmas funções públicas
de `Database` ou crie uma implementação equivalente; o plugin não muda.
