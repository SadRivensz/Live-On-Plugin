# Catálogos da Live On

- `bosses.json`: nomes exibidos, métrica do Wise Old Man e `iconItemId` usado como ícone.
- `ranks.json`: tradução dos ranks e indicação de quais ranks são staff.
- `items.json`: somente regras e aliases específicos do clã.

Os milhares de itens do jogo não são duplicados aqui. O plugin usa `ItemManager`,
que acompanha automaticamente o cache do RuneLite após atualizações do jogo.
Os ícones de bosses usam itens do cache pelo `iconItemId`, evitando PNGs
duplicados. Quando um boss novo ainda não estiver no catálogo, a interface usa
um troféu neutro. Ícones próprios de ranks podem ficar em `resources/ranks`.
