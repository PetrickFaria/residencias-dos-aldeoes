from hashlib import sha256
from pathlib import Path
import struct

texture=Path('src/main/resources/assets/residenciasaldeoes/textures/item/emerald_fragment.png')
data=texture.read_bytes()
assert data[:8]==b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>II',data[16:24])==(16,16)
# Pinned output is a direct green recolor of the 1.19.2 vanilla amethyst-shard
# silhouette: 89 opaque pixels and the original shading/highlight distribution.
assert sha256(data).hexdigest()=='c11d41a0d8e6aa76bb662ff3c9679e35834b7a8d35725e7ed0bd8b73cb6d4123'
model=Path('src/main/resources/assets/residenciasaldeoes/models/item/emerald_fragment.json').read_text()
assert 'residenciasaldeoes:item/emerald_fragment' in model
print('Emerald Fragment texture verified: pinned 16x16 recolor of vanilla 1.19.2 amethyst shard.')
