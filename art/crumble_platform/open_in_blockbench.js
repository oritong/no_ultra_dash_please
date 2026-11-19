(() => {
  const allowed = ['crumble_platform', 'crumble_platform_v1', 'crumble_platform_v2', 'crumble_platform_v3', 'crumble_platform_collapsed'];
  if (!Project || !allowed.includes(Project.name) || Format.id !== 'java_block' || Outliner.elements.length || Texture.all.length) {
    throw Error('Create a new empty Java block project with one of the five crumble platform names.');
  }
  const fs = require('fs');
  const file = 'D:/MCMOD/no_ultra_dash_please/art/crumble_platform/' + Project.name + '.bbmodel';
  const source = JSON.parse(fs.readFileSync(file, 'utf8'));
  Project.save_path = file;
  Undo.initEdit({elements: [], textures: [], outliner: true});
  Codecs.project.parse(source, file);
  Undo.finishEdit('Open crumble platform artwork for review', {elements: Cube.all.slice(), textures: Texture.all.slice(), outliner: true});
  Canvas.updateAll();
  Project.saved = true;
  return {name: Project.name, uuid: Project.uuid, elements: Cube.all.length, textures: Texture.all.length, path: file};
})()
