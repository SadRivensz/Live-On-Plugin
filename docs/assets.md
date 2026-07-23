# Catalogs and icons

[English](assets.md) | [Brazilian Portuguese](assets.pt-BR.md)

The catalogs are stored in `src/main/resources/catalog`.

## Items

RuneLite already maintains the complete item library through `ItemManager` and
the IDs in `net.runelite.api.gameval.ItemID`. Duplicating the entire list would
make it stale after game updates. `items.json` contains only clan-specific
exceptions:

```json
{
  "itemId": 123,
  "alias": "Name used by Live On",
  "alwaysNotify": true,
  "iconOverride": "items/123.png"
}
```

## Bosses and ranks

Add the JSON entry and, when a custom image is required, place an optimized PNG
in `resources/bosses` or `resources/ranks`. A catalog entry may omit an image
while the frontend uses text or a RuneLite-provided icon.

Avoid oversized images: Java decodes complete PNG files into memory. Prefer
32–64 px images for side-panel icons.
