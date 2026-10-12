# UV de diagnóstico do VillagerModel — Java 1.19.2

## O que foi inspecionado

A versão 1.19.2 usa o VillagerModel vanilla; a aplicação customizada não converte o aldeão em modelo de jogador. O modelo contém cabeça, nariz, peças de chapéu/aba, tronco, braços cruzados e pernas. O renderer da skin continua usando o modelo já instalado no renderer do aldeão.

O atlas de referência é 64×64. O PNG de diagnóstico colore os retângulos UV amostrados por cada caixa do modelo. A origem é o canto superior esquerdo; coordenadas iniciam em zero e os limites à direita/baixo não são incluídos.

## Ilhas e cores do PNG de teste

| Região | Cor no diagnóstico | Offset UV e tamanho 3D | Área geral do atlas |
|---|---|---|---|
| Cabeça base | Vermelho | (0, 0), 8×10×8 | x=0–32, y=0–18 |
| Chapéu/camada externa da cabeça | Violeta | (32, 0), 8×10×8 | x=32–64, y=0–18 |
| Nariz | Amarelo | (24, 0), 2×4×2 | x=24–32, y=0–6 |
| Tronco base | Azul | (16, 20), 8×12×6 | x=16–44, y=20–38 |
| Casaco/caixa externa do tronco | Ciano | (0, 38), 8×18×6 | x=0–28, y=38–62 |
| Braços cruzados | Verde | (44, 22), 4×8×4 por braço, UV espelhado/compartilhado | x=44–60, y=22–34 |
| Conector dos braços | Laranja | (40, 38), 8×4×4 | x=40–64, y=38–46 |
| Pernas | Rosa | (0, 22), 4×12×4 por perna, UV espelhado/compartilhado | x=0–16, y=22–38 |
| Aba do chapéu | Branco | (30, 47), 16×16×1, geometria rotacionada | x=30–64, y=47–64 |

Os retângulos gerais incluem pixels de alinhamento das faces. Algumas caixas reutilizam UV com espelhamento; o mesmo desenho será repetido no membro espelhado. A tabela face a face no README mostra coordenadas específicas para frente, costas, lados, topo e base das caixas principais.

## Usar o diagnóstico

- villager_uv_diagnostic_64.png é o atlas pequeno para comparação direta.
- villager_uv_diagnostic_256.png é a mesma imagem ampliada por vizinho mais próximo, sem interpolação.
- Os blocos sólidos intencionais têm alpha 255. O restante é transparente (alpha 0). Use o PNG somente para teste: as cores são diagnósticas, não uma skin pronta.

## Imagem enviada para comparação

skeleton-villager-custom.png tem 64×64 pixels e canal RGBA. Na cópia examinada, 1.176 pixels têm alpha 255, 2.920 têm alpha 0, nenhum tem alpha intermediário e não há pixel preto opaco. Vários pixels totalmente transparentes carregam RGB preto, mas esse RGB deve ser ignorado pelo alpha. Isso prova que preto visível não está gravado como pixel preto opaco no arquivo recebido; não prova, sem executar o jogo, como cada pixel aparece no renderer.

## Resultado da investigação do código

- A pele é enviada por UUID e registrada como DynamicTexture; o layer usa getParentModel() do renderer do aldeão e entityCutoutNoCull. O mod não cria uma segunda instância de modelo nem usa modelo de jogador. Há um passe adicional de textura sobre o modelo que o renderer já desenhou, não uma troca da textura base do renderer.
- O loader anterior decodificava os tamanhos aceitos, mas copiava pixels de 128/256/512 para uma imagem 64×64. Isso perdia detalhes. UVs da geometria de roupas/acessórios também eram assados sempre com atlas 64×64, apesar das texturas maiores. Ambos foram corrigidos: o PNG base permanece na resolução original; para cubos adicionais, offset UV e tamanho do atlas baked são escalados em conjunto.
- Alpha é mantido sem composição RGB. O passe cutout descarta pixels alfa zero; como a textura base vanilla já foi desenhada, esses pontos transparentes deixam a base aparecer. Não dá para concluir apenas pelo PNG qual pixel escuro percebido no jogo vem da transparência ou de outra camada.
- O código anterior procurava e envolvia a VillagerProfessionLayer, que em 1.19.2 gerencia mapas tanto por tipo/bioma como por profissão. A camada é relevante mesmo para aldeões desempregados. O rearranjo refletivo foi tornado transacional: a camada customizada não é removida antes de preparar a lista substituta; o log informa quando a camada vanilla não foi encontrada ou não pôde ser envolvida. Skin Completa omite a camada combinada apenas para o residente com modo Completo. Skin Parcial a delega.
- O cliente Minecraft não foi iniciado nesta execução. As imagens são recursos para verificação futura, não evidência de validação visual no jogo.


## Cobertura da skin skeleton enviada

A contagem abaixo usa as ilhas de superfície da caixa do modelo (pixels únicos de UV), não a caixa retangular inteira do PNG. O resultado explica por que o corpo base aparece apenas parcialmente customizado: a textura de exemplo deixa essas superfícies transparentes.

| Ilha | Pixels de superfície UV | Opacos na amostra | Transparentes |
|---|---:|---:|---:|
| Cabeça base | 448 | 412 | 36 |
| Sobreposição da cabeça/chapéu | 448 | 0 | 448 |
| Nariz | 40 | 40 | 0 |
| Tronco base | 432 | 168 | 264 |
| Casaco externo do modelo | 600 | 0 | 600 |
| Mangas/braços | 160 | 160 | 0 |
| Braços cruzados | 160 | 160 | 0 |
| Pernas | 224 | 224 | 0 |
| Aba do chapéu | 576 | 0 | 576 |

Até a versão 1.0.22, Skin Completa ocultava a camada vanilla combinada de tipo/bioma e profissão, mas o render do aldeão ainda desenhava primeiro a textura base do modelo. O passe cutout adicional descartava alpha zero e a textura vanilla anterior permanecia visível nesses pixels.

## Investigação da captura da versão 1.0.22

O passe customizado era inserido antes das camadas vanilla registradas. Isso permitia que uma camada posterior voltasse a pintar sobre os pixels opacos do PNG. A conferência direta da face frontal, UV `(8,8)` com tamanho `8×10`, encontrou 80/80 pixels opacos; portanto, os pixels cinza/pretos do rosto, incluindo os olhos da textura enviada, podem ser desenhados diretamente do PNG se ele for a textura base do modelo. Esse é um indício verificável de que a ordem de renderização e a arquitetura de overlay contribuíam para as diferenças observadas; não identifica quais camadas específicas produziram cada pixel da captura.

A frente do tronco, UV `(22,26)` com tamanho `8×12`, possui 42/96 pixels opacos, e a frente do casaco `(6,44)` com tamanho `8×18` possui 0/144. A partir da 1.0.23, o Mixin faz o PNG ser a textura do passe base para Skin Completa; por isso os pixels transparentes deixam de expor a textura vanilla previamente desenhada e mostram a cena atrás. A inspeção estática confirma conteúdo e cobertura UV, não substitui um teste no cliente nem confirma a aparência final dentro do Minecraft.

O teste reproduzível `python3 tools/verify_skin_reference.py examples/skeleton-villager-custom-validation.png` valida as regiões faciais e do torso usadas neste diagnóstico.



O Mixin intercepta somente `VillagerRenderer.getTextureLocation` no cliente e apenas quando o UUID tem uma skin em modo Completo. Skin Parcial segue o caminho anterior. A camada do mod continua necessária para acessórios e peças de vestuário adicionadas por pacotes.
