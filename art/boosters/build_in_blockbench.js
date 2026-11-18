/* Select an empty Generic project named booster / red_booster / either _occupied. */
(() => {
  const allowed = ['booster', 'red_booster', 'booster_occupied', 'red_booster_occupied'];
  if (!Project || !allowed.includes(Project.name) || Format.id !== 'free' || Outliner.elements.length || Texture.all.length) {
    throw Error('Select one of the four empty booster projects.');
  }
  const fs = require('fs'), root = 'D:/MCMOD/no_ultra_dash_please', name = Project.name;
  const source = JSON.parse(fs.readFileSync(root + '/art/boosters/models/' + name + '.mesh.json', 'utf8'));
  const path = root + '/art/boosters/textures/' + name + '.png';
  Undo.initEdit({elements: [], textures: [], outliner: true});
  const t = new Texture({name: name + '.png', namespace: 'no_ultra_dash_please', folder: 'block',
    uv_width: 16, uv_height: 16, render_sides: 'front'}).fromDataURL('data:image/png;base64,' + fs.readFileSync(path).toString('base64')).add();
  t.path = path; t.saved = true;
  const m = new Mesh({name: name + '_shell', vertices: source.vertices, faces: {}, shading: 'flat'});
  source.faces.forEach(face => m.addFaces(new MeshFace(m, {...face, texture: t.uuid})));
  m.init(); Project.texture_width = Project.texture_height = 16;
  Canvas.updateAll();
  Undo.finishEdit('Create low-poly booster membrane and native pixel material', {elements: [m], textures: [t], outliner: true});
  const scale = Settings.get('model_export_scale');
  let obj = Codecs.obj.compile({mtl_name: name + '.mtl'});
  obj = obj.split('\n').map(line => line.startsWith('v ')
    ? 'v ' + line.slice(2).split(/\s+/).map((v, i) => Number(v) * scale / 16 + (i === 1 ? 0 : 0.5)).join(' ')
    : line.startsWith('usemtl ') ? 'usemtl membrane' : line).join('\n');
  fs.writeFileSync(root + '/src/main/resources/assets/no_ultra_dash_please/models/block/' + name + '.obj', obj + '\n');
  Project.save_path = root + '/art/boosters/models/' + name + '.bbmodel';
  fs.writeFileSync(Project.save_path, JSON.stringify(Codecs.project.compile({raw: true, bitmaps: true}), null, 2) + '\n');
  Project.saved = true;
  return {path: Project.save_path, vertices: Object.keys(m.vertices).length, faces: Object.keys(m.faces).length};
})()
