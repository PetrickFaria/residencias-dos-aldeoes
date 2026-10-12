# Pacote de personalização de aldeão — schema 1

Um pacote é um ZIP importado da pasta `config/residenciasaldeoes/skins`. PNGs simples continuam aceitos sem acessórios. O pacote não executa código: contém somente imagens e um manifesto declarativo.

## Arquivos

- `manifest.json` — schema e referências às imagens/modelos.
- `base.png` — textura na UV do aldeão vanilla.
- `outer.png` — atlas independente para jaqueta/mangas/roupa externa.
- `headphones.png` e `hood.png` — texturas dos acessórios.

Cada PNG quadrado deve ter 64, 128, 256 ou 512 pixels. A resolução original e o canal alfa são preservados. Coordenadas de UV no manifesto continuam sendo escritas em unidades do atlas 64×64; ao carregar uma textura maior, o modelo de cubos multiplica os offsets e a dimensão do atlas pela mesma escala, sem redimensionar o arquivo. O nearest-neighbor sem mipmaps mantém o pixel art.

## Manifesto

```json
{
  "schema": 1,
  "base_texture": "base.png",
  "outer_texture": "outer.png",
  "outer_parts": [
    {"anchor":"BODY","pivot":[0,0,0],"rotation":[0,0,0],"cubes":[{"from":[-4.25,0,-3.25],"size":[8.5,12.25,6.5],"uv":[16,20]}]},
    {"anchor":"ARMS","pivot":[0,0,0],"rotation":[0,0,0],"cubes":[{"from":[-8.25,-4.25,-4.25],"size":[16.5,8.5,8.5],"uv":[44,22]}]}
  ],
  "accessories": [
    {"anchor":"HEAD","pivot":[0,0,0],"rotation":[0,0,0],"texture":"headphones.png","cubes":[{"from":[-4,-10.5,-1],"size":[8,1,2],"uv":[0,0]}]}
  ]
}
```

`anchor` pode ser `HEAD`, `BODY`, `ARMS`, `RIGHT_LEG` ou `LEFT_LEG`. Coordenadas e dimensões usam pixels de modelo (16 pixels = 1 bloco); rotações são em radianos. `from` indica o canto inicial e `size` deve conter dimensões positivas. Cada cubo tem `uv` `[u,v]` em escala de 64. `outer_parts` usa `outer_texture` por padrão, mas pode sobrescrever `texture`; cada acessório deve declarar seu próprio PNG.

## Limites de segurança

Até 1,5 MB comprimidos, 3 MB descompactados, 64 entradas ZIP, 32 arquivos, manifesto de 32 KB, 24 partes por grupo e 32 cubos por parte. Caminhos absolutos, `..`, ZIPs duplicados e imagens em dimensões não suportadas são rejeitados. O servidor valida o pacote antes de persistir; ele é armazenado e sincronizado por UUID junto com o modo Skin Completa/Parcial.

O modelo do aldeão não é substituído. As geometrias adicionais são cubos presos ao `ModelPart` indicado, então seguem a pose vanilla. Modelos externos alterados por EMF/Fresh Animations ainda precisam de verificação dentro do jogo; as partes de corpo/nariz originais permanecem vanilla.
