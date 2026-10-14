from pathlib import Path
import os, hashlib, subprocess, shutil

version='1.0.49'
out=Path('artifact-output');out.mkdir(exist_ok=True)
jar=Path(f'build/libs/residencias-dos-aldeoes-{version}.jar')
shutil.copy2(jar,out/jar.name)
subprocess.run(['git','archive','--format=zip','--output='+str(out/f'residencias-dos-aldeoes-{version}-source.zip'),'HEAD'],check=True)
for name in ['gametest-console.log','client-console.log','client-restore-console.log']:
    p=Path(name)
    if p.exists():shutil.copy2(p,out/name)
if Path('build/screenshots').exists():shutil.copytree('build/screenshots',out/'screenshots',dirs_exist_ok=True)
sha=os.environ['GITHUB_SHA']
(out/f'relatorio-{version}.md').write_text(f"""# Residências dos Aldeões {version}

Minecraft Java 1.19.2 / Forge 43.5.0 / Java 17.

- Preview de residência: removido envio de faces por BufferUploader privado; preenchimentos usam RenderType dedicado e buffer compartilhado. Contornos e dimensões preservados.
- Porta Reforçada: comportamento de abertura herdado de DoorBlock, pequeno afastamento dos reforços metálicos das faces externas e ícone GUI afinado; modelos do mundo preservados.
- Geração de golens e gatos: aldeões residentes excluídos apenas dos critérios de população pertinentes.
- Camas: reivindicações exclusivas verificadas no início do sono e tratadas em conflitos/ocupação temporária.

Commit: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
