# Referência de skin de aldeão — Java 1.19.2

O PNG-base do mod é renderizado no `VillagerModel` vanilla. A prévia cria uma entidade `Villager` e usa o mesmo `EntityRenderDispatcher`/`VillagerRenderer` que a entidade no mundo; a skin completa substitui a textura-base e as peças de guarda-roupa usam o modelo parental desse renderizador. O atlas de referência tem base UV 64×64. Para uma imagem quadrada de lado `N`, o carregador mantém os mesmos UVs e amplia a resolução por `N/64`: ×4 em 256 e ×16 em 1024. As dimensões aceitas são 64, 128, 256, 512 e 1024.

## Arquivos

- `villager_skin_template_256.png` e `villager_skin_template_1024.png`: canvas RGBA completamente transparente, pronto para usar em uma camada de pintura.
- `villager_uv_regions_guide.png`: mapa colorido com os nomes das partes e os avisos de transparência.
- `villager_uv_guide_1024.png`: atlas ampliado em 1024×1024 para consulta de pixels.
- `aldeao_creeper_256_corrigido.png`: a textura anexada com somente as duas áreas de chapéu/aba sem uso tornadas transparentes.

## Coordenadas UV do `VillagerModel`

As faixas abaixo usam coordenadas na imagem 64×64, com o limite final excluído. Multiplique cada coordenada por 4 para 256×256 ou por 16 para 1024×1024.

| Parte | UV (x0, y0)–(x1, y1) | Observação |
|---|---|---|
| Cabeça base | várias faces no atlas x=0–32, y=0–18 | Frente do rosto: x=8–16, y=8–18; topo: x=16–24, y=0–8. |
| Nariz | x=24–32, y=0–6 | Face frontal do nariz: x=26–28, y=2–6. |
| Sobreposição da cabeça/chapéu | x=32–64, y=0–18 | Camada externa elevada 0,51 bloco. Deixe alpha 0 sem acessório. |
| Corpo base | x=16–44, y=20–38 | Caixa 8×12×6. |
| Pernas | x=0–16, y=22–38 | Caixa 4×12×4; os dois lados compartilham UVs espelhados. |
| Jaqueta/manto | x=0–28, y=38–64 | Caixa externa 8×20×6. |
| Braços laterais | x=44–60, y=22–34 | Caixa 4×8×4; braços espelhados compartilham UVs. |
| Braços cruzados | x=40–64, y=38–46 | Caixa 8×4×4. |
| Aba larga do chapéu | x=30–64, y=47–64 | Peça plana 16×16×1. Deixe alpha 0 sem aba. |

As faces da cabeça e do corpo são planificações de cubos; o guia separa as faces da cabeça e mostra as caixas UV completas para as demais partes.

## Transparência e acessórios

Textura de pele/roupa deve ocupar apenas as faces correspondentes. Alpha 0 fora de uma face não cria geometria. Se o aldeão não tiver um chapéu ou aba, mantenha transparentes os dois UVs externos vermelhos no guia: x=32–64,y=0–18 e x=30–64,y=47–64. Pixels opacos nesses pontos são renderizados como as peças externas reais do modelo, podendo formar projeções laterais que parecem “chapéus invisíveis”. Não inclua quadriculado de editor na imagem; transparência é o canal alpha do PNG.

O mod não troca o modelo vanilla. EMF/Fresh Animations pode substituí-lo por um CEM próprio e alterar UVs. O adaptador existente do Fresh Animations 1.8 só preenche a ilha facial que esse modelo espera; o template aqui documenta o atlas vanilla do mod, não UVs arbitrários de outros pacotes de modelo.

## Textura Creeper recebida

A textura de 256×256 já segue em geral a distribuição do atlas vanilla e tem alpha real. No arquivo recebido havia pixels visíveis nas duas ilhas de chapéu (a caixa externa da cabeça e a aba larga). Como o conceito não tem chapéu, esses pixels eram aplicados nas peças reais do modelo e produziam as tiras que aparecem nas capturas. A cópia corrigida preserva todos os pixels RGB e todos os demais alphas; só zera o alpha dentro desses dois UVs. Confira primeiro sem Fresh Animations; modelos CEM de terceiros podem usar outro mapeamento.
