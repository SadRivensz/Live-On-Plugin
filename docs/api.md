# Contrato da API

Todas as rotas após `/v1/auth/verify` exigem:

```http
Authorization: Bearer <token>
```

## Autenticação

`POST /v1/auth/verify`

```json
{
  "rsn": "Player Name",
  "clanName": "Live On",
  "clanRank": "member",
  "accessCode": "codigo-da-staff",
  "pluginVersion": "0.1.0"
}
```

O servidor valida código, roster e staff. O token não é salvo em disco pelo
plugin e expira por padrão em 12 horas.

## Eventos

- `POST /v1/drops`
- `POST /v1/pets`
- `POST /v1/collection-log`

O backend é responsável por persistir e gerar embeds. Não aceite `webhookUrl`
enviado pelo cliente.

## Leitura

- `GET /v1/announcements`
- `GET /v1/drops?limit=25`
- `GET /v1/members?query=nome`
- `GET /v1/members/profile?rsn=nome`
- `GET /v1/rankings?metric=xp&period=current`

Métricas: `xp`, `ehp`, `ehb`. Períodos: `current`, `previous`.

## Staff

`POST /v1/announcements` exige token com `staff=true`.

```json
{
  "title": "Evento de sábado",
  "message": "Encontro no GE às 20h.",
  "kind": "clan",
  "showOnLogin": true
}
```
