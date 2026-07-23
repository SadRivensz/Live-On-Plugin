# Live On catalogs

[English](README.md) | [Brazilian Portuguese](README.pt-BR.md)

- `bosses.json`: display names, Wise Old Man metrics and the `iconItemId` used
  for each icon.
- `ranks.json`: clan-rank display names and staff permissions.
- `items.json`: clan-specific rules and aliases only.

The plugin does not duplicate the game's thousands of items. It uses
`ItemManager`, which follows RuneLite's cache automatically after game updates.
Boss icons use cache items through `iconItemId`, avoiding duplicate PNG files.
When a new boss is not yet in the catalog, the interface uses a neutral trophy.
Custom rank icons may be stored in `resources/ranks`.
