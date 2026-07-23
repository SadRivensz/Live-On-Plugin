# Security and privacy

[English](security.md) | [Brazilian Portuguese](security.pt-BR.md)

## Data sent by the plugin

Only after a member enables the integration:

- the RSN and clan rank used for authorization;
- the source, items and value of drops above the configured threshold;
- local game messages that identify a pet or new Collection Log entry;
- a drop screenshot when screenshot capture is enabled.

The server sees the member's IP address as it would for any HTTPS request. The
integration is therefore disabled by default and includes the disclosure
required by the Plugin Hub.

## Data that must never be included in the plugin

- Discord webhook;
- backend HMAC secret;
- Wise Old Man or RuneProfile API keys;
- a hardcoded plain-text access code;
- RuneScape or Jagex account credentials.

## Known limitations

- Local clan-chat membership confirms context, but the backend remains the final
  authority.
- A shared access code may leak. Server-side RSN validation and short-lived
  tokens reduce its impact. Discord/RSN linking can replace the shared code in a
  future version.
- Historical Collection Log data is not public through Wise Old Man. The
  project stores newly observed client entries; importing historical data
  requires explicit consent and another source.
- Production deployments must use HTTPS and should rate-limit requests at the
  reverse proxy.
