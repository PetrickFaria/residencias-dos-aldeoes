from pathlib import Path
import os, hashlib, subprocess, shutil

out=Path('artifact-output');out.mkdir(exist_ok=True)
jar=Path('build/libs/residencias-dos-aldeoes-1.0.46.jar')
shutil.copy2(jar,out/jar.name)
subprocess.run(['git','archive','--format=zip','--output='+str(out/'residencias-dos-aldeoes-1.0.46-source.zip'),'HEAD'],check=True)
for name in ['gametest-console.log','client-console.log','client-restore-console.log']:
    p=Path(name)
    if p.exists():shutil.copy2(p,out/name)
if Path('build/screenshots').exists():shutil.copytree('build/screenshots',out/'screenshots',dirs_exist_ok=True)
sha=os.environ['GITHUB_SHA']
(out/'relatorio-1.0.46.md').write_text(f"""# Residências dos Aldeões 1.0.46

Base concluída: 1.0.45, commit 9116c220ace4cd19578fd015c4f24f80cb84e456.
Minecraft Java 1.19.2, Forge 43.5.0, Java 17.

Transporte: chegada em níveis adjacentes ao bloco (inclusive embutido no piso), espaço livre para o corpo do aldeão, raio mínimo de oito blocos para baús carregados, somente rotas alcançáveis, primeiro residente disponível e apenas um transportador por residência. Pagamento de 200 ticks e espera de 1.200 ticks preservados.

Porta: duas janelas de vidro transparente, madeira central, moldura metálica completa, placas nos quatro cantos e rebites tridimensionais. Estados e interação/collision/receita originais preservados.

Baú: face de madeira com moldura dourada e única esmeralda retangular facetada presa à tampa. Cavidade geométrica real com quatro paredes, fundo rebaixado e bordas. Laterais/traseira/topo aprovados e inventário de 27 slots preservados.

Dez GameTests obrigatórios, incluindo amanhecer com bloco embutido no piso e baú fora do raio antigo, falta de baú, caminho bloqueado/desbloqueado e primeiro residente indisponível. Testes anteriores de pagamento, dez slots, migração, baú cheio, várias viagens, recuperação de carga, porta e drops mantidos.

As capturas em screenshots permitem inspeção da porta, cavidade e interface. Reinício de mundo validado quando client-restore-console.log contém TEN_SLOTS_WORLD_RESTART_PASSED. Não foram testados modpacks ou o mundo particular do usuário.

Commit de compilação: {sha}.
SHA-256 JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
""",encoding='utf-8')
print('Delivery prepared:',jar.name,'commit',sha)
