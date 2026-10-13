from pathlib import Path
import os, re, json, hashlib, subprocess, shutil, base64

out=Path('artifact-output');out.mkdir(exist_ok=True)
jar=Path('build/libs/residencias-dos-aldeoes-1.0.44.jar')
shutil.copy2(jar,out/jar.name)
subprocess.run(['git','archive','--format=zip','--output='+str(out/'residencias-dos-aldeoes-1.0.44-source.zip'),'HEAD'],check=True)
for name in ['gametest-console.log','client-console.log']:
    p=Path(name)
    if p.exists():shutil.copy2(p,out/name)
screens=Path('build/screenshots')
if screens.exists():shutil.copytree(screens,out/'screenshots',dirs_exist_ok=True)
log=Path('gametest-console.log').read_text(errors='replace')
sample=re.search(r'DROP_SAMPLE size=10000 two_fragments=(\d+) one_fragments=(\d+)',log)
sample_text=(sample.group(2)+' resultados de 1 fragmento e '+sample.group(1)+' de 2 fragmentos') if sample else 'Amostra registrada no log.'
sha=os.environ['GITHUB_SHA']
report=f"""# Residências dos Aldeões — 1.0.44

Base: ZIP original 1.0.43 anexado nesta solicitação, sem uso das correções parciais.
Minecraft Java 1.19.2, Forge 43.5.0, Java 17.

## Alteração 1 — Transporte dos Fragmentos
- Resultado: implementado e verificado no servidor Forge GameTest.
- Explicação: primeiro residente, agendamento de 1.200 ticks, navegação física com prioridade exclusiva, coleta de até 64 fragmentos por viagem, depósitos sequenciais e recuperação da carga. A geração e o pagamento originais foram preservados. Busca inclui baús no mesmo nível e um bloco abaixo.

## Alteração 2 — Porta Reforçada
- Resultado: orientação visual corrigida nas quatro direções.
- Explicação: estados visuais alinhados aos modelos vanilla; interação, colisão e texturas originais preservadas. Testes acionam a abertura e o fechamento e comparam colisões para ambas as dobradiças.

## Alteração 3 — Baú Esmerálico
- Resultado: modelo personalizado único, com corpo fixo e tampa móvel.
- Explicação: geometria e texturas do Bloco Residencial reutilizadas; tampa, molduras, esmeralda superior e pequeno fecho facetado movem-se juntos. Slots do menu rejeitam itens inválidos antes de consumir o inventário do jogador. Inventário de 27 slots e identificadores mantidos.

## Alteração 4 — Drops dos Aldeões
- Resultado: 90% para exatamente 1 fragmento; 10% para exatamente 2.
- Explicação: sorteio exclusivo, sem zero, sem efeito de Saque e sem aplicação a bebês, aldeões zumbis ou comerciantes ambulantes. Testes incluem mortes ambientais, por jogador e por criatura em adultos comuns e residentes.
- Amostra no Minecraft: {sample_text}, total de 10.000 sorteios.

## Verificação
- Compilação: sucesso, JAR reobfuscado para Forge.
- GameTest: seis testes obrigatórios executados no servidor Minecraft; aprovação exigida pelo workflow.
- Servidor Minecraft: executado via Forge GameTest.
- Cliente gráfico: tentativa real com Xvfb; quando presentes, as capturas ficam na pasta screenshots para inspeção.
- Cenários: pagamento por amanhecer comandado, espera de um minuto, seleção do primeiro residente, percurso físico, baú cheio, pagamentos consecutivos, múltiplas viagens, preservação por NBT, morte do transportador, inserção inválida com destino vazio, porta nas quatro direções e drops.
- Não realizados: descarregamento real de chunks, reinício completo com carga em trânsito, compatibilidade com modpacks e teste separado de redstone. Esses casos não são apresentados como testes executados.
- O servidor informa a conclusão dos testes; o workflow trata somente a falha de encerramento do daemon que ocorre após a confirmação explícita de sucesso.

## Entrega
- Versão: 1.0.44.
- Branch: fix/v1.0.44-transport-door-chest-drops.
- Commit de código: {sha}.
- JAR individual: residencias-dos-aldeoes-1.0.44.jar.
- Código-fonte completo: residencias-dos-aldeoes-1.0.44-source.zip.
- SHA-256 do JAR: {hashlib.sha256(jar.read_bytes()).hexdigest()}.
"""
(out/'relatorio-1.0.44.md').write_text(report,encoding='utf-8')

for name,label in [('visual-chests.png','SCREENSHOT_CHESTS_BASE64'),('visual-doors.png','SCREENSHOT_DOORS_BASE64')]:
    p=Path('build/screenshots')/name
    if p.exists():
        encoded=base64.b64encode(p.read_bytes()).decode()
        for index in range(0,len(encoded),4096):print(label+'_CHUNK '+str(index//4096).zfill(6)+' '+encoded[index:index+4096])
