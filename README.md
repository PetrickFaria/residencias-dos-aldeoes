# Mod de Residências dos Aldeões 1.0.23

Minecraft Java 1.19.2 · Forge 43.5.0 · Java 17.

## Skins PNG para o aldeão vanilla

As skins usam o UV do `VillagerModel` do Minecraft 1.19.2. PNGs quadrados de 64, 128, 256 ou 512 pixels são aceitos e mantidos na resolução original. O modelo vanilla armazena UVs normalizados a partir do atlas 64×64, então a mesma disposição proporcional funciona nas quatro resoluções; o cliente não reduz a imagem. A filtragem nearest-neighbor sem mipmaps preserva os pixels. Não são skins de jogador: o modelo, nariz, braços cruzados, pernas e animações continuam sendo os do aldeão.

Há duas maneiras de disponibilizar PNGs:

1. Copie o arquivo para `config/residenciasaldeoes/skins/`.
2. Ou coloque-o em um pacote de recursos ativo em `assets/residenciasaldeoes/skins/`.

Use somente o nome do arquivo como identificador, sem `.png`, com letras, números, ponto, hífen ou sublinhado (até 64 caracteres). Ao abrir **Personalização → Alterar Skin**, a galeria lê a pasta de configuração e os pacotes de recursos ativos. Se o mesmo identificador existir nos dois locais, o PNG da pasta `config` prevalece. PNGs simples devem ter até 1,5 MB. Para roupa externa e acessórios, use um pacote ZIP compatível conforme a documentação abaixo. Reabra a tela depois de instalar ou alterar um arquivo.

Os arquivos [diagnóstico UV 64×64](docs/villager_uv_diagnostic_64.png) e [diagnóstico UV 256×256](docs/villager_uv_diagnostic_256.png) usam cores distintas para cabeça, nariz, tronco, braços, pernas e partes externas. São texturas de teste: aplique-as a um aldeão para ver onde as ilhas aparecem no modelo. A [legenda detalhada](docs/diagnostico-uv-aldeao-1.19.2.md) identifica faces e offsets. Remova as cores antes de criar uma skin final.

| Peça do modelo vanilla | UV inicial e tamanho da caixa | Faces no atlas (x, y, largura, altura) |
|---|---|---|
| Cabeça base | `(0,0)`, 8×10×8 | Esquerda `(0,8,8,10)`, frente `(8,8,8,10)`, direita `(16,8,8,10)`, costas `(24,8,8,10)`, topo `(8,0,8,8)`, base `(16,0,8,8)` |
| Nariz | `(24,0)`, 2×4×2 | Frente `(26,2,2,4)`; faces laterais, costas, topo e base ocupam o restante da ilha `(24,0)`–`(32,6)` |
| Aba/chapéu da cabeça | `(32,0)`, 8×10×8 | Frente `(40,8,8,10)`, costas `(56,8,8,10)`, laterais `(32,8,8,10)` e `(48,8,8,10)`, topo `(40,0,8,8)`, base `(48,0,8,8)` |
| Tronco base | `(16,20)`, 8×12×6 | Frente `(22,26,8,12)`, costas `(36,26,8,12)`, laterais `(16,26,6,12)` e `(30,26,6,12)`, topo `(22,20,8,6)`, base `(30,20,8,6)` |
| Braços externos (UV compartilhado e espelhado) | `(44,22)`, 4×8×4 | Frente `(48,26,4,8)`; faces laterais, costas, topo e base dentro de `(44,22)`–`(60,34)` |
| Braços cruzados | `(40,38)`, 8×4×4 | Frente `(44,42,8,4)`; demais faces dentro de `(40,38)`–`(64,46)` |
| Pernas (UV compartilhado e espelhado) | `(0,22)`, 4×12×4 | Frente `(4,26,4,12)`; faces laterais, costas, topo e base dentro de `(0,22)`–`(16,38)` |
| Casaco/sobreposição do tronco | `(0,38)`, 8×18×6 | Frente `(6,44,8,18)`; demais faces dentro de `(0,38)`–`(28,62)` |
| Aba do chapéu | `(30,47)`, 16×16×1, rotacionada no modelo | Ilha dentro de `(30,47)`–`(64,64)` |

As peças de chapéu e casaco são partes/camadas do modelo de aldeão. O desenho da skin deve cobrir as ilhas correspondentes quando quiser personalizar essas áreas. A galeria valida o tamanho e o formato PNG; ela não converte texturas de jogadores nem altera a geometria.

Cada textura e modo é associado ao UUID escolhido e salvo nos dados do mundo. **Skin Completa** é o padrão e omite, somente para esse aldeão, a camada vanilla combinada de roupa do bioma/profissão e acessórios/nível. **Skin Parcial** aplica o PNG à base e preserva essa camada vanilla. O modo pode ser alternado sem importar o arquivo novamente. A opção **Skin Original** remove a substituição; desvincular o morador também remove a skin gerenciada. O canal alfa original é mantido; pixels transparentes seguem o comportamento cutout do Minecraft e deixam a textura base abaixo aparecer. O modelo e as animações vanilla continuam sendo usados.

## Modos de skin (1.0.18)

- A captura anexada mostrava a face customizada e o corpo vanilla. O código da 1.0.17 desenhava o PNG numa RenderLayer antes da VillagerProfessionLayer. Esta camada vanilla também desenha vestimenta por tipo de bioma, inclusive em aldeões desempregados; portanto, as camadas de profissão não eram a única causa.
- A camada do mod permanece no renderer/modelo vanilla existente, mas agora a camada combinada vanilla é delegada por um wrapper que só a omite para residentes que tenham uma skin e estejam em modo Completo. Parcial continua delegando a renderização vanilla normalmente.
- O modo e o PNG são sincronizados junto ao UUID e persistidos em SavedData. Saves anteriores sem `skin_mode` recebem o modo Completo ao carregar. A alteração de modo não retransmite o PNG ao servidor.
- O canal alfa original continua preservado, com filtragem nearest-neighbor sem mipmaps. O renderizador, o modelo, os dados de profissão, as trocas, a IA e a geometria não são substituídos.
- A galeria mantém a aparência aprovada e oferece os seletores Completa/Parcial.

## Correção de inicialização

O manipulador de desconexão agora é registrado no barramento Forge. Os eventos de ciclo de vida do cliente e de camadas de renderização permanecem no barramento MOD.

## Compilação

Com Java 17, execute `./gradlew clean build`. O JAR fica em `build/libs/residencias-dos-aldeoes-1.0.23.jar`.


## Personalização em camadas (1.0.19)

Skins PNG antigas seguem suportadas. Para camadas externas e acessórios, consulte [o formato de pacote](docs/personalizacao-pacote-v1.md) e use o ZIP de exemplo em `examples/pacote-jaqueta-fones.zip`. Copie pacotes ou PNGs compatíveis para `config/residenciasaldeoes/skins` e selecione no menu **Personalização → Alterar Skin**.

Pacotes são ZIPs com manifesto JSON e PNGs; o sistema valida caminhos, tamanhos, dimensões, número de componentes e cubos antes de persistir. Os dados são salvos e sincronizados por UUID. Camadas extras usam cubos ligados às partes vanilla `HEAD`, `BODY`, `ARMS`, `RIGHT_LEG` e `LEFT_LEG`.

A prévia de personalização permite arrastar o aldeão para girá-lo horizontalmente e usar **Centralizar** para voltar à frente.

## Correção de skins (1.0.22)

A imagem personalizada é registrada em sua resolução nativa, sem reduzir 128/256/512 para 64 pixels. Pacotes de roupas/acessórios também escalam os UVs declarados em base 64 ao assar a geometria adicional, para que uma textura maior continue cobrindo o mesmo atlas completo. A camada de skin usa o renderer e o modelo vanilla atuais. Ela é um passe de textura sobre o modelo renderizado pelo jogo, não uma substituição do renderer; áreas totalmente transparentes são descartadas pelo modo cutout e revelam a textura base que foi desenhada antes.

## Ordem de renderização das skins (1.0.23)

Skin Completa agora substitui a textura base retornada pelo `VillagerRenderer` por um hook Mixin localizado. O PNG participa do passe normal do renderer sobre o mesmo modelo já animado, em vez de ser uma segunda cópia do corpo. Isso permite que o alpha transparente mostre o fundo da cena, sem revelar a textura vanilla que antes era desenhada sob o overlay. A camada vanilla combinada de bioma/profissão continua suprimida apenas para esse aldeão. Roupas externas e acessórios do pacote são desenhados depois da textura base.

O alpha continua sendo preservado. Na textura de referência, a face frontal (UV x=8–15, y=8–17) tem 80/80 pixels opacos; a frente do tronco (x=22–29, y=26–37) tem 42/96 opacos e a ilha frontal do casaco (x=6–13, y=44–61) tem 0/144. O hook de textura base impede que esses pixels transparentes revelem a textura vanilla de baixo. O cliente do Minecraft não foi iniciado nesta sessão, então a reprodução da captura alvo e a compatibilidade com EMF/ETF/Fresh Animations ainda não estão validadas visualmente.


## Ajustes visuais (1.0.20)

A prévia 3D da tela de skins é centralizada e recortada dentro do painel escuro, mantendo rotação interativa e a área do botão **Centralizar** separada. O modelo residencial recebeu cantos de madeira maiores, um aro elevado no topo e esmeraldas laterais com faces triangulares reais. O ID do bloco e seus dados persistidos permanecem inalterados.
