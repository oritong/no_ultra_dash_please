(() => {
  if (!Project || !['booster_outline', 'red_booster_outline'].includes(Project.name) || Format.id !== 'java_block'
      || Outliner.elements.length || Texture.all.length) throw Error('Select a matching empty Java block project.');
  const fs = require('fs');
  const file = 'D:/MCMOD/no_ultra_dash_please/art/boosters/models/' + Project.name + '.bbmodel';
  Project.save_path = file;
  Undo.initEdit({elements: [], textures: [], outliner: true});
  Codecs.project.parse(JSON.parse(fs.readFileSync(file, 'utf8')), file);
  Undo.finishEdit('Load dashed booster outline', {elements: Cube.all.slice(), textures: Texture.all.slice(), outliner: true});
  Canvas.updateAll();
  Project.saved = true;
  return {name: Project.name, uuid: Project.uuid, cubes: Cube.all.length, textures: Texture.all.length};
})()
