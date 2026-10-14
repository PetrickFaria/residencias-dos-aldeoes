from pathlib import Path
import os, hashlib, subprocess, shutil

out=Path('artifact-output');out.mkdir(exist_ok=True)
jar=Path('build/libs/residencias-dos-aldeoes-1.0.47.jar')
shutil.copy2(jar,out/jar.name)
subprocess.run(['git','archive','--format=zip','--output='+str(out/'residencias-dos-aldeoes-1.0.47-source.zip'),'HEAD'],check=True)
for name in ['gametest-console.log','client-console.log','client-restore-console.log']:
    p=Path(name)
    if p.exists():shutil.copy2(p,out/name)
if Path('build/screenshots').exists():shutil.copytree('build/screenshots',out/'screenshots',dirs_exist_ok=True)
sha=os.environ['GITHUB_SHA']
(out/'relatorio-1.0.47.md').write_text(f"""# Residências dos Aldeões 1.0.47

Base: 1.0.46, f9be18fefee71cd06221df785d08e63c89f946fa. Minecraft 1.19.2 / Forge 43.5.0 / Java 17.

- Localização: índice por dimensão/chunk de Baús Esmerálicos carregados, atualizado ao carregar/remover/descarregar; raio de 1.000 blocos medido a partir do Bloco Residencial. Sem varrer milhões de blocos e sem carregar chunks para buscar. Tentativas de seleção espaçadas em 40 ticks. A localização é distinta da possibilidade de percorrer o caminho.
- Velocidade de navegação durante transporte: 0,8 → 0,5. Atributos e comportamentos fora da tarefa preservados.
- Tampa: interação do aldeão agrega um contador temporário ao contador vanilla dos jogadores. Usa o mesmo evento/controlador de tampa, com dez ticks para abertura antes do depósito e vinte ticks de permanência após concluir. Leases expiram e não persistem através do save.
- Texturas: refinamento discreto no tamanho e alpha originais. Texturas compartilhadas do baú foram copiadas e seus vínculos de materiais ajustados; elementos, meshes e estados geométricos aprovados preservados.
- Doze GameTests obrigatórios; testes de registro/localização 5, 20, 100, 500 e 1.000 blocos separados dos testes de deslocamento. Testes NPC isolados em batches porque agora baús de outros cenários ficam dentro do alcance de 1.000 blocos.
- Cliente gráfico: capturas e marcadores de abertura/fechamento e interação simultânea com jogador exigidos. Reinício de mundo preservando os dez slots verificado por marcador próprio.
- Não foi executado um percurso completo de 500 ou 1.000 blocos nem o mundo/modpack do usuário.

Commit: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
