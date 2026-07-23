# Catálogos e ícones

[English](assets.md) | [Português (Brasil)](assets.pt-BR.md)

Os catálogos ficam em `src/main/resources/catalog`.

## Itens

O RuneLite já mantém a biblioteca completa de itens em `ItemManager` e nos IDs
de `net.runelite.api.gameval.ItemID`. Duplicar a lista inteira criaria dados
obsoletos após cada atualização. `items.json` guarda apenas exceções do clã:

```json
{
  "itemId": 123,
  "alias": "Nome usado pela Live On",
  "alwaysNotify": true,
  "iconOverride": "items/123.png"
}
```

## Bosses e ranks

Adicione a entrada JSON e, se houver imagem própria, coloque um PNG otimizado em
`resources/bosses` ou `resources/ranks`. Caminhos ausentes podem continuar no
catálogo enquanto o frontend usa texto ou ícones do RuneLite.

Evite imagens enormes: o Java decodifica PNGs inteiros na memória. Para ícones
da barra lateral, prefira 32–64 px.
