# Mod de Residências dos Aldeões 1.0.17

Minecraft Java 1.19.2 · Forge 43.5.0 · Java 17.

## Skins PNG para o aldeão vanilla

As skins são texturas **64×64 pixels no UV do `VillagerModel` de Minecraft 1.19.2**. Elas não usam o UV nem o modelo de jogador. O modelo vanilla continua controlando nariz, cabeça, braços cruzados, pernas e animações; a textura escolhida é renderizada no próprio modelo do aldeão.

Há duas maneiras de disponibilizar PNGs:

1. Copie o arquivo para `config/residenciasaldeoes/skins/`.
2. Ou coloque-o em um pacote de recursos ativo em `assets/residenciasaldeoes/skins/`.

Use somente o nome do arquivo como identificador, sem `.png`, com letras, números, ponto, hífen ou sublinhado (até 64 caracteres). Ao abrir **Personalização → Alterar Skin**, a galeria lê a pasta de configuração e os pacotes de recursos ativos. Se o mesmo identificador existir nos dois locais, o PNG da pasta `config` prevalece. Os PNGs devem ter 64×64 e até 256 KiB. Reabra a tela depois de instalar ou alterar um arquivo.

O arquivo [modelo_uv_aldeao_vanilla_1.19.2.png](docs/modelo_uv_aldeao_vanilla_1.19.2.png) é um guia de cores UV em tamanho real (64×64). Substitua as cores de guia ao pintar; não o selecione como skin final. A imagem é acompanhada pelo mapa dos retângulos de UV abaixo. Os intervalos usam coordenadas começando em zero e o limite final não está incluído.

| Peça do modelo vanilla | UV inicial e tamanho da caixa | Faces no atlas (x, y, largura, altura) |
|---|---|---|
| Cabeça base | `(0,0)`, 8×10×8 | Esquerda `(0,8,8,10)`, frente `(8,8,8,10)`, direita `(16,8,8,10)`, costas `(24,8,8,10)`, topo `(8,0,8,8)`, base `(16,0,8,8)` |
| Nariz | `(24,0)`, 2×4×2 | Frente `(26,2,2,4)`; faces laterais, costas, topo e base ocupam o restante da ilha `(24,0)`–`(32,6)` |
| Aba/chapéu da cabeça | `(32,0)`, 8×12×8 | Frente `(40,8,8,12)`, costas `(56,8,8,12)`, laterais `(32,8,8,12)` e `(48,8,8,12)`, topo `(40,0,8,8)`, base `(48,0,8,8)` |
| Tronco base | `(16,20)`, 8×12×6 | Frente `(22,26,8,12)`, costas `(36,26,8,12)`, laterais `(16,26,6,12)` e `(30,26,6,12)`, topo `(22,20,8,6)`, base `(30,20,8,6)` |
| Braços externos (UV compartilhado e espelhado) | `(44,22)`, 4×8×4 | Frente `(48,26,4,8)`; faces laterais, costas, topo e base dentro de `(44,22)`–`(60,34)` |
| Braços cruzados | `(40,38)`, 8×4×4 | Frente `(44,42,8,4)`; demais faces dentro de `(40,38)`–`(64,46)` |
| Pernas (UV compartilhado e espelhado) | `(0,22)`, 4×12×4 | Frente `(4,26,4,12)`; faces laterais, costas, topo e base dentro de `(0,22)`–`(16,38)` |
| Casaco/sobreposição do tronco | `(0,38)`, 8×18×6 | Frente `(6,44,8,18)`; demais faces dentro de `(0,38)`–`(28,62)` |
| Aba do chapéu | `(30,47)`, 16×16×1, rotacionada no modelo | Ilha dentro de `(30,47)`–`(64,64)` |

As peças de chapéu e casaco são partes/camadas do modelo de aldeão. O desenho da skin deve cobrir as ilhas correspondentes quando quiser personalizar essas áreas. A galeria valida o tamanho e o formato PNG; ela não converte texturas de jogadores nem altera a geometria.

Cada textura é atribuída ao UUID escolhido e salva/sincronizada pelos dados do mundo. A opção **Skin Original** remove a substituição; desvincular o morador também remove a skin gerenciada. O canal alfa original é mantido; pixels transparentes seguem o comportamento cutout do Minecraft e deixam a textura base abaixo aparecer. A skin é desenhada antes das camadas normais de profissão e nível, que continuam sendo renderizadas depois.

## Correção 1.0.17 da skin

- Removido o preenchimento forçado do canal alfa, que convertia áreas transparentes em pixels pretos opacos.
- A camada usa filtragem nearest-neighbor sem mipmaps para preservar o pixel art.
- A skin é posicionada no início da lista de camadas do renderizador existente. Roupa/acessórios de profissão e insígnias continuam sendo desenhados pelas camadas vanilla por cima; o renderizador e o modelo de aldeão não são substituídos.
- Trocas com PNG válido atualizam a textura dinâmica do UUID selecionado. Texturas anteriores são substituídas/liberadas pelo TextureManager; reset e logout liberam texturas.

## Correção de inicialização

O manipulador de desconexão agora é registrado no barramento Forge. Os eventos de ciclo de vida do cliente e de camadas de renderização permanecem no barramento MOD.

## Compilação

Com Java 17, execute `./gradlew clean build`. O JAR fica em `build/libs/residencias-dos-aldeoes-1.0.17.jar`.
