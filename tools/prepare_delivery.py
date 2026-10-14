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

Base: 1.0.47, f538ce8e2c066cb98cebb8578e9d9641814e23a8. Minecraft 1.19.2 / Forge 43.5.0 / Java 17.

- A coleta começa 300 ticks (15 segundos) depois da disponibilização do pagamento; pagamento e quantidade diária permanecem inalterados.
- Cada viagem leva até 15 Fragmentos de Esmeralda; a tarefa continua em viagens sequenciais até concluir a coleta. Recuperação via baú cheio e morte do transportador segue preservada.
- Porta Reforçada: pequeno afastamento dos reforços metálicos em relação às faces externas para eliminar superfícies coplanares; ícone GUI usa escala 1.0. Orientação, texturas e proporção da porta no mundo preservadas.
- Preview residencial usa buffer de renderização compartilhado e transformação local isolada por residência, sem construir preenchimentos no Tesselator global.
- Aldeões residentes dormindo têm navegação, alvo de caminhada e velocidade residual limpos após o passo de IA, sem forçar despertar.

Commit: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
