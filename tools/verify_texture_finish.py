from pathlib import Path
from verify_reinforced_door import png_rgba
root=Path('src/main/resources/assets/residenciasaldeoes');sources=Path('tools/texture_sources')
for source in sources.glob('*.png'):
    target=root/'textures/item/reinforced_door.png' if source.name=='reinforced_door_item.png' else root/'textures/block'/source.name
    assert png_rgba(source)==png_rgba(target),str(target)
for target in (root/'textures/block').glob('chest_finish_*.png'):
    source=target.with_name(target.name.removeprefix('chest_finish_'))
    assert png_rgba(source)==png_rgba(target),str(target)
print('Texture dimensions and every alpha value preserved; glass transparency and emerald silhouettes unchanged.')
