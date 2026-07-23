# Contrato da API

[English](api.md) | [Português (Brasil)](api.pt-BR.md)

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
  "pluginVersion": "0.2.7"
}
```

O servidor valida código, roster e staff. O token não é salvo em disco pelo
plugin e expira por padrão em 12 horas.

## Eventos

- `POST /v1/drops`
- `POST /v1/pets`
- `POST /v1/collection-log`

O backend é responsável por persistir e gerar embeds. Não aceite `webhookUrl`
enviado pelo cliente. Drops podem incluir `screenshotBase64` (PNG/JPEG, até 7 MB
depois de decodificado); o servidor salva a imagem em `data/screenshots`.

## Leitura

- `GET /v1/announcements`
- `GET /v1/drops?limit=25`
- `GET /v1/members?query=nome`
- `GET /v1/members/profile?rsn=nome`
- `GET /v1/members/runeprofile?rsn=nome`
- `GET /v1/rankings?metric=xp&period=current`
- `GET /v1/item-goal`

Métricas: `xp`, `ehp`, `ehb`. Períodos: `current`, `previous`.

O RuneProfile é consultado separadamente do perfil WOM para que Skills, Bosses
e Atividades apareçam sem aguardar o Collection Log completo. O endpoint retorna
`404` quando não há perfil público e usa cache para respeitar o limite da API
externa.

## Item desejado

- `PUT /v1/item-goal` marca ou substitui o objetivo ativo do usuário.
- `DELETE /v1/item-goal` remove o objetivo ativo.

```json
{
  "itemId": 23956,
  "itemName": "Enhanced crystal weapon seed"
}
```

Quando um drop contém o ID ou nome marcado, o servidor conclui o objetivo,
calcula o tempo de busca e cria um anúncio `item_goal` para todos os membros.

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
