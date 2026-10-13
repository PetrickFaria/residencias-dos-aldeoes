from pathlib import Path
import os, hashlib, subprocess, shutil

out=Path('artifact-output');out.mkdir(exist_ok=True)
jar=Path('build/libs/residencias-dos-aldeoes-1.0.45.jar')
shutil.copy2(jar,out/jar.name)
subprocess.run(['git','archive','--format=zip','--output='+str(out/'residencias-dos-aldeoes-1.0.45-source.zip'),'HEAD'],check=True)
for name in ['gametest-console.log','client-console.log']:
    p=Path(name)
    if p.exists():shutil.copy2(p,out/name)
if Path('build/screenshots').exists():shutil.copytree('build/screenshots',out/'screenshots',dirs_exist_ok=True)
sha=os.environ['GITHUB_SHA']
(out/'relatorio-1.0.45.md').write_text(f"""# Residências dos Aldeões 1.0.45

Base: 1.0.44, commit 0ba1f29556f5a99633de3fb41929d720bc1e9b0c.
Minecraft Java 1.19.2, Forge 43.5.0, Java 17.

- Dez slots de saída, uma fileira inferior centralizada (y=210; intervalo=21 pixels) na interface de 272×232, sem deslocar outros componentes.
- Slots antigos preservam os índices 0–4; novos índices 5–9 começam vazios ao carregar dados antigos.
- Produção completa pilhas existentes antes de usar espaços vazios. Capacidade de 640 fragmentos e excedente persistido.
- Coleta percorre os dez slots; retirada normal/shift-click preservada e inserção manual bloqueada.
- Pagamento, agendamento, navegação, residentes, skins, modelos, baú, porta, drops e receitas preservados.
- Build compilado; sete GameTests obrigatórios, incluindo migração, armazenamento/recarregamento NBT, excedente, interação do menu e transporte com dez slots.
- Captura da interface real do cliente, quando disponível, em screenshots/visual-anchor-slots.png. Exige inspeção da imagem antes de declarar validação visual.
- NBT e reconstrução da entidade de bloco testados; não foi realizado um reinício completo do processo do mundo, teste de modpacks ou descarregamento real de chunks.

Commit de compilação: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
