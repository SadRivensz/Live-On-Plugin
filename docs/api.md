# API contract

[English](api.md) | [Brazilian Portuguese](api.pt-BR.md)

Every route after `/v1/auth/verify` requires:

```http
Authorization: Bearer <token>
```

## Authentication

`POST /v1/auth/verify`

```json
{
  "rsn": "Player Name",
  "clanName": "Live On",
  "clanRank": "member",
  "accessCode": "staff-access-code",
  "pluginVersion": "0.2.7"
}
```

The server validates the access code, roster and staff status. The plugin does
not persist the token to disk, and the token expires after 12 hours by default.

## Events

- `POST /v1/drops`
- `POST /v1/pets`
- `POST /v1/collection-log`

The backend persists events and builds Discord embeds. Never accept a
`webhookUrl` supplied by the client. Drops may include `screenshotBase64`
(PNG/JPEG, up to 7 MB after decoding); the server stores the image in
`data/screenshots`.

## Read operations

- `GET /v1/announcements`
- `GET /v1/drops?limit=25`
- `GET /v1/members?query=name`
- `GET /v1/members/profile?rsn=name`
- `GET /v1/members/runeprofile?rsn=name`
- `GET /v1/rankings?metric=xp&period=current`
- `GET /v1/item-goal`

Metrics: `xp`, `ehp`, `ehb`. Periods: `current`, `previous`.

RuneProfile is queried separately from the Wise Old Man profile so Skills,
Bosses and Activities do not wait for the full Collection Log response. The
endpoint returns `404` when no public profile exists and caches responses to
respect the external API rate limit.

## Item goal

- `PUT /v1/item-goal` creates or replaces the user's active goal.
- `DELETE /v1/item-goal` removes the active goal.

```json
{
  "itemId": 23956,
  "itemName": "Enhanced crystal weapon seed"
}
```

When a drop contains the selected item ID or name, the server completes the
goal, calculates the hunt duration and creates an `item_goal` announcement for
all members.

## Staff

`POST /v1/announcements` requires a token with `staff=true`.

```json
{
  "title": "Saturday event",
  "message": "Meet at the Grand Exchange at 8 PM.",
  "kind": "clan",
  "showOnLogin": true
}
```
