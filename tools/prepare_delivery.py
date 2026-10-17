from pathlib import Path
import os, hashlib, subprocess, shutil, re

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
game_test_log=Path('gametest-console.log')
match=re.search(r'All (\d+) required tests passed',game_test_log.read_text(encoding='utf-8',errors='replace')) if game_test_log.exists() else None
game_test_result=f"{match.group(1)} Forge GameTests aprovados" if match else "resultado Forge GameTest não identificado no log"
(out/f'relatorio-{version}.md').write_text(f"""# Residências dos Aldeões {version}

Minecraft Java 1.19.2 / Forge 43.5.0 / Java 17.

- Renderização de limites: um único listener em AFTER_TRANSLUCENT_BLOCKS; contornos e preenchimentos usam uma fonte de buffer privada por frame e as coordenadas das caixas foram mantidas.
- Bloco Residencial: painel verde com facetas discretas; modelo, dimensões e colisão preservados.
- Baú Esmerálico: manteve as texturas refinadas já presentes e a animação/27 slots; não foi alterada a receita.
- Fragmento: ícone 16x16 com silhueta e distribuição de pixels do fragmento de ametista vanilla 1.19.2, recolorido em verde; identificador e receita preservados.
- Notificações: mortes de residentes usam o apelido e causa identificável; conversão em bruxa é separada e remove a associação residencial. Mensagens são limitadas à dimensão.
- Camas: a reserva é mantida por residente; camas quebradas, fora dos limites ou sem aproximação navegável são liberadas, e a busca só ocorre durante a rotina noturna espaçada.
- Golens: removido o bloqueio do mod ao método vanilla de elegibilidade; nenhum sistema de geração artificial foi adicionado. A presença de condições naturais precisa ser verificada no mundo de jogo.
- Configuração: config/residenciasaldeoes-common.toml; padrões 3, 200, 300, 15 e 1000. Reinicie o jogo/servidor para reler o TOML.
- Capture to Mob: identidade persistente para reconciliar um residente recriado com outro UUID; duplicatas com o original carregado são recusadas; sem dependência obrigatória.
- Opacidade Steve: a passagem base do PlayerModel usa o passe translúcido do residente, e o alfa existente continua aplicado também às camadas da skin.
- Rotina noturna sem cama: caminhadas e pausas variam entre 3 e 7 segundos; destinos permanecem dentro da residência e rejeitam rotas que cruzem portas de madeira.
- Portas noturnas: o trajeto de fechamento continua prioritário; a caminhada sem cama evita reabrir a porta que acabou de ser fechada.
- GameTests Forge: {game_test_result}, incluindo transporte, persistência, camas e notificações.
- Limitação: GameTests não simulam a validação gráfica nem garantem que o mundo reúna as condições naturais de geração de golens.

Commit: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
