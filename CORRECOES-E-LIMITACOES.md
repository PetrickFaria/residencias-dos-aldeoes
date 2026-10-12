# Revisão 1.0.8

## Correções

- A lista vazia vinha da guarda de tempo do menu: calcular `tickAtual - Long.MIN_VALUE` transbordava e resultava negativo, bloqueando o primeiro pacote S2C mesmo com a contagem sendo enviada pelos `DataSlot`. A guarda agora só aplica a janela de 10 ticks depois do primeiro envio. O menu também responde a um pedido explícito de snapshot ao abrir a tela.
- A tela e o renderizador recebem juntos o estado permanente confirmado pelo servidor. A exibição usa uma única decisão por quadro (`permanente || dentro do volume`); fora do volume e com opção desligada, não renderiza. O envio continua limitado a âncoras carregadas e próximas, sem carregar chunks.
- A desvinculação usa estado explícito de nomes: nome original/visibilidade original e apelido residencial atual. Restauração ocorre ao desvincular um residente, todos ou destruir a âncora, e também após o retorno de residentes descarregados.
- A lista visível foi reduzida para cinco linhas, com recorte compatível com a GUI do Minecraft 1.19.2, rolagem pela roda e barra vertical clicável/arrastável. Os botões permanecem fixos sob a lista; a tela foi reduzida para 226 px de altura.

- Limites automáticos ativos somente dentro do volume residencial, avaliados localmente por quadro; opção ligada preserva exibição fora do volume até o limite de renderização.
- Sincronização de âncoras próximas com o estado permanente por jogador e âncora; nenhuma chunk é carregada para renderizar limites.
- GUI de moradores com linhas persistidas, seleção por UUID, estado vazio sincronizado ao abrir, rolagem e ações após registro/renomeação/desvinculação/morte.
- Renomeação em janela modal com foco de teclado, confirmação e cancelamento; nomes armazenados e reaplicados ao aldeão quando ele volta a carregar.
- Botões reagrupados conforme a organização solicitada.
- Foram preservadas a desvinculação total que não apaga configurações, os tamanhos, o bloco verde sólido, receita, drops, teletransporte, contenção e reserva exclusiva de camas.

## Verificação executada

- Build limpo com OpenJDK 17.0.20, Gradle 7.5.1, ForgeGradle 5.1.77 e Forge 43.5.0: `BUILD SUCCESSFUL`.
- A tarefa `test` indicou `NO-SOURCE`; não há testes automatizados no projeto.
- Os testes 1–6 exigem interação no cliente Minecraft e não foram executados. Nenhum comportamento foi declarado testado dentro do jogo.
- Saves 1.0.7 não guardavam o nome anterior para residentes já renomeados; nesses registros antigos sem `name_state`, não há como distinguir com segurança um apelido alterado de um nome personalizado anterior. Nomes residenciais registrados/renomeados nesta versão são guardados com o original e restaurados corretamente.
# Versão 1.0.15 — Personalização e skins PNG

- O botão principal anteriormente chamado **Renomear** agora se chama **Personalização** e mantém sua posição, tamanho, paleta e estilo.
- O submenu oferece **Renomear**, **Alterar Skin** e **Voltar**. A seleção do residente continua vinculada ao UUID durante a navegação.
- A galeria lê PNGs 64×64 em `config/residenciasaldeoes/skins/`. A alteração é confirmada ou cancelada antes de enviar os bytes ao servidor.
- Cada skin é persistida nos dados da residência pelo UUID, replicada aos clientes e desenhada sobre o mesmo `VillagerModel` vanilla, preservando animações, formato, IA, profissão e trocas.
- Skin Original, desvinculação individual/coletiva, morte e destruição da âncora removem a textura gerenciada; o render vanilla reaparece sem alterar a entidade.
- Dois PNGs demonstrativos são empacotados no mod e copiados para a pasta de configuração ao abrir a galeria pela primeira vez.

## Versão 1.0.17 — Correção inicial das skins

- A imagem RGBA é preservada, a textura usa filtragem nearest-neighbor e a camada é posicionada antes das camadas vanilla.
- Essa mudança não resolveu a cobertura da vestimenta vanilla no corpo. A causa foi identificada na 1.0.18: a camada de profissão do aldeão também aplica a roupa do bioma a aldeões desempregados.

## Verificações e limitações da versão 1.0.17

- Revisão estática do diff contra a fonte 1.0.16: as mudanças de execução estão limitadas a `VillagerSkinTextures`, `VillagerSkinLayer` e `ClientSetup`; os outros arquivos atualizados são os metadados de versão e esta documentação.
- Revisão estática confirma que o PNG não tem o canal alfa forçado a opaco, a validação continua exigindo 64×64, a filtragem está em nearest-neighbor sem mipmaps e a layer continua usando `VillagerModel`.
- A compilação foi tentada primeiro sem rede e novamente com acesso de rede solicitado. A tentativa escalada também falhou antes da compilação, agora com `UnknownHostException: services.gradle.org`; o DNS do ambiente não resolve o host oficial do Gradle.
- A tentativa offline com um diretório Gradle novo não dispunha do pacote de distribuição; o wrapper precisa baixar o Gradle 7.5.1 antes de avaliar as dependências do projeto.
- A distribuição alternativa local do Gradle 7.5.1 também não pôde ser usada: `gradle-kotlin-dsl-7.5.1.jar` está corrompido (`ZipException: zip END header not found`). Não foi encontrado cache local utilizável do Gradle/Forge para compilar sem rede.
- Foi preparado `.github/workflows/build.yml` com Ubuntu, Temurin Java 17, Gradle Wrapper do projeto e upload do JAR como artefato; o workflow ainda não foi executado porque não há conexão autorizada a um repositório GitHub nesta sessão.
- O GitHub está disponível para conexão como integração, mas não está instalado/conectado. Criar ou usar um repositório privado e enviar o código requer autorização/conexão da conta do proprietário; nenhum repositório foi criado e nenhum código foi publicado.
- A versão 1.0.17 foi compilada com sucesso no workflow GitHub Actions usando Temurin 17 e Gradle Wrapper 7.5.1; o JAR baixado foi validado como ZIP e continha `mods.toml`, classes e recursos da versão 1.0.17.
- O cliente Minecraft não foi iniciado. Os testes A–G, incluindo renderização vanilla, PNG com regiões distintas, transparência, profissões, persistência, remoção e compatibilidade com Fresh Animations/EMF/ETF, permanecem pendentes de validação em jogo. A imagem recebida é uma captura RGB, não o arquivo de skin PNG original, e não permite validar sua paleta alfa/UV.
- Os testes visuais no jogo não foram realizados nessa versão.

## Versão 1.0.18 — Skin Completa e Skin Parcial

- `VillagerSkinLayer` continua usando o modelo vanilla original. Para modo Completo, um wrapper de `VillagerProfessionLayer` não delega a camada combinada de roupa de bioma/profissão e acessórios somente quando o UUID tem skin completa ativa. No modo Parcial, a camada original é delegada sem alterações.
- `SkinApplicationMode`, `ResidenceData`, `ModNetwork`, `AnchorMenu`, `VillagerSkinTextures` e `AnchorScreen` guardam, sincronizam e mostram o modo por aldeão. Saves antigos sem modo são lidos como Completo; a remoção da skin e a desvinculação limpam o PNG e o modo junto.
- A reflexão permanece localizada à lista de camadas do renderer porque o evento Forge `AddLayers` só acrescenta camadas; é usada para inserir a skin antes da camada vanilla e envolvê-la sem substituir o renderer ou o modelo. Se um renderer de terceiro impedir esse acesso/refletir sua lista, o log avisa que o modo Completo pode não conseguir ocultar a roupa.
- A persistência, sincronização, seleção por UUID e UV continuam dependendo do PNG 64×64 do modelo de aldeão. O cliente de jogo não foi executado nesta sessão; os testes visuais A–H permanecem pendentes até validar dentro do Minecraft, inclusive com Fresh Animations/EMF/ETF.
+

## Versão 1.0.21 — Resolução nativa, UV e diagnóstico das skins

- `VillagerSkinTextures` não reduz mais arquivos 128×128, 256×256 ou 512×512 para 64×64. O PNG é entregue ao DynamicTexture nas dimensões originais; alpha é preservado e a filtragem segue nearest-neighbor sem mipmaps.
- Texturas de roupa e acessórios em pacotes ZIP também preservam resolução. Os offsets UV em escala base 64 são multiplicados pela escala da imagem e o `LayerDefinition` é assado com atlas proporcional. Isso evita amostrar apenas parte da imagem maior.
- `VillagerSkinLayer` ainda usa o renderer e o modelo do aldeão em um passe de textura adicional. Não existe uma segunda instância de modelo. A camada vanilla combinada de tipo/bioma e profissão é ignorada apenas em Skin Completa. A substituição da lista de render layers agora é preparada antes de mutá-la; o log comunica quando a camada vanilla não pôde ser encontrada ou envolvida.
- `docs/villager_uv_diagnostic_64.png` e `docs/villager_uv_diagnostic_256.png` são texturas de teste por região. `examples/skeleton-villager-custom-validation.png` guarda uma cópia do PNG enviado para análise. O relatório UV explica offsets e alpha.

### Verificações da 1.0.21

- Inspeção estática confirmou que o PNG de exemplo tem 64×64 RGBA, 1.176 pixels opacos, 2.920 transparentes, zero pixels alfa intermediário e zero pixels pretos opacos. O RGB de pixels totalmente transparentes não deve produzir preto visível; no passe cutout esses pixels deixam aparecer a textura base desenhada antes.
- A contagem UV da amostra confirmou falhas de cobertura que explicam a roupa base visível: 264/432 pixels da ilha de tronco são transparentes e 600/600 da caixa externa do casaco são transparentes. A sobreposição de cabeça e aba do chapéu também estão completamente transparentes (448/448 e 576/576). A planilha completa está em `docs/diagnostico-uv-aldeao-1.19.2.md`.
- O gerador UV executou e criou atlas 64×64 e 256×256 por nearest-neighbor. A imagem de referência foi conferida por dimensão, modo e alpha.
- `VillagerProfessionLayer` mantém estado/caches por tipo de aldeão e por profissão, então a mesma camada pode atuar em aldeões desempregados. A implementação mantém Skin Parcial delegando a camada vanilla e Skin Completa ignorando-a somente para o residente personalizado.
- Minecraft não foi iniciado. Os testes visuais A–J de renderização, persistência após reinício, troca de recursos, Fresh Animations, EMF e ETF não foram executados. Não há testes JUnit no projeto (`test` é `NO-SOURCE`).

