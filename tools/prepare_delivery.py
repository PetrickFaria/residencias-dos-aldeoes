from pathlib import Path
import os, hashlib, subprocess, shutil

version=dict(line.split('=',1) for line in Path('gradle.properties').read_text().splitlines() if '=' in line)['mod_version']
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

- Renderização de limites: um único listener em AFTER_TRANSLUCENT_BLOCKS; contornos e preenchimentos usam uma fonte de buffer privada por frame e as coordenadas das caixas foram mantidas.
- Bloco Residencial: painel verde com facetas discretas; modelo, dimensões e colisão preservados.
- Baú Esmerálico: manteve as texturas refinadas já presentes e a animação/27 slots; não foi alterada a receita.
- Fragmento: textura 16x16 redesenhada como cristal irregular; identificador e receita preservados.
- Configuração: config/residenciasaldeoes-common.toml; padrões 3, 200, 300, 15 e 1000. Reinicie o jogo/servidor para reler o TOML.
- Capture to Mob: identidade persistente para reconciliar um residente recriado com outro UUID; duplicatas com o original carregado são recusadas; sem dependência obrigatória.
- GameTests Forge: 16 testes aprovados, incluindo persistência de inventário, transporte, range e rebind de identidade.

Commit: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
