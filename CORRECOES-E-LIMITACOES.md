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

## Versão 1.0.17 — Correção de transparência e ordem das camadas

- A causa do preto em pixels transparentes era a substituição de cada alfa pelo valor 255 em `VillagerSkinTextures.receive`.
- A camada personalizada era adicionada ao fim da lista e re-renderizava o modelo inteiro; assim, cobria os acessórios profissionais, inclusive o chapéu de fazendeiro.
- Agora a imagem RGBA é preservada, a textura usa filtragem nearest-neighbor e a camada é movida para antes das camadas vanilla existentes, que seguem renderizando sobre a skin.
- O renderizador e o modelo não são substituídos; a associação por UUID, a persistência e a sincronização existentes permanecem.

## Verificações e limitações da versão 1.0.17

- Revisão estática do diff contra a fonte 1.0.16: as mudanças de execução estão limitadas a `VillagerSkinTextures`, `VillagerSkinLayer` e `ClientSetup`; os outros arquivos atualizados são os metadados de versão e esta documentação.
- Revisão estática confirma que o PNG não tem o canal alfa forçado a opaco, a validação continua exigindo 64×64, a filtragem está em nearest-neighbor sem mipmaps e a layer continua usando `VillagerModel`.
- A compilação foi tentada primeiro sem rede e novamente com acesso de rede solicitado. A tentativa escalada também falhou antes da compilação, agora com `UnknownHostException: services.gradle.org`; o DNS do ambiente não resolve o host oficial do Gradle.
- A tentativa offline com um diretório Gradle novo não dispunha do pacote de distribuição; o wrapper precisa baixar o Gradle 7.5.1 antes de avaliar as dependências do projeto.
- A distribuição alternativa local do Gradle 7.5.1 também não pôde ser usada: `gradle-kotlin-dsl-7.5.1.jar` está corrompido (`ZipException: zip END header not found`). Não foi encontrado cache local utilizável do Gradle/Forge para compilar sem rede.
- Foi preparado `.github/workflows/build.yml` com Ubuntu, Temurin Java 17, Gradle Wrapper do projeto e upload do JAR como artefato; o workflow ainda não foi executado porque não há conexão autorizada a um repositório GitHub nesta sessão.
- O GitHub está disponível para conexão como integração, mas não está instalado/conectado. Criar ou usar um repositório privado e enviar o código requer autorização/conexão da conta do proprietário; nenhum repositório foi criado e nenhum código foi publicado.
- Portanto, a versão 1.0.17 não foi compilada nesta sessão e não existe um JAR 1.0.17 validado para entregar. O JAR 1.0.16 anterior não contém estas correções e não deve ser usado como substituto.
- O cliente Minecraft não foi iniciado. Os testes A–G, incluindo renderização vanilla, PNG com regiões distintas, transparência, profissões, persistência, remoção e compatibilidade com Fresh Animations/EMF/ETF, permanecem pendentes de validação em jogo. A imagem recebida é uma captura RGB, não o arquivo de skin PNG original, e não permite validar sua paleta alfa/UV.
- Não foi executada uma compilação/teste automatizado nesta sessão; as verificações acima são estáticas. A compilação é o próximo gate necessário antes da instalação.
