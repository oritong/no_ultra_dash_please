/* Run in the empty refill_crystal_empty Generic Model project in Blockbench. */
(() => {
  if (Project?.name !== 'refill_crystal_empty' || Format.id !== 'free' || Outliner.elements.length || Texture.all.length) {
    throw Error('Select an empty refill_crystal_empty Generic Model project.');
  }
  const fs = require('fs'), root = 'D:/MCMOD/no_ultra_dash_please';
  const art = root + '/art/refill_crystal_empty';
  const assets = root + '/src/main/resources/assets/no_ultra_dash_please';
  const source = JSON.parse(fs.readFileSync(root + '/art/refill_crystal/refill_crystal.bbmodel', 'utf8'));
  if (source.elements.length !== 1 || source.elements[0].type !== 'mesh' || Object.keys(source.elements[0].faces).length !== 8) {
    throw Error('Expected the saved filled crystal with eight triangular faces.');
  }
  const pixels = JSON.parse(fs.readFileSync(art + '/texture.pixel.json', 'utf8'));
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 16;
  const ctx = canvas.getContext('2d');
  pixels.rows.forEach((row, y) => {
    if (row.length !== 16) throw Error('Pixel rows must be exactly 16 characters.');
    [...row].forEach((pixel, x) => {ctx.fillStyle = pixels.palette[pixel]; ctx.fillRect(x, y, 1, 1);});
  });
  const path = assets + '/textures/block/refill_crystal_empty.png';
  fs.writeFileSync(path, Buffer.from(canvas.toDataURL().split(',')[1], 'base64'));
  Undo.initEdit({elements: [], textures: [], outliner: true});
  const texture = new Texture({name: 'refill_crystal_empty.png', namespace: 'no_ultra_dash_please', folder: 'block',
    uv_width: 16, uv_height: 16, render_sides: 'front'}).fromDataURL(canvas.toDataURL()).add();
  texture.path = path;
  texture.saved = true;
  texture.updateMaterial();
  const meshData = JSON.parse(JSON.stringify(source.elements[0]));
  delete meshData.uuid;
  meshData.name = 'refill_crystal_empty_octahedron';
  for (const face of Object.values(meshData.faces)) face.texture = texture.uuid;
  const mesh = new Mesh(meshData).init();
  Project.texture_width = Project.texture_height = 16;
  Undo.finishEdit('Create empty crystal with white interrupted outline', {elements: [mesh], textures: [texture], outliner: true});
  Canvas.updateAll();
  const scale = Settings.get('model_export_scale');
  let obj = Codecs.obj.compile({mtl_name: 'refill_crystal_empty.mtl'});
  obj = obj.split('\n').map(line => line.startsWith('v ')
    ? 'v ' + line.slice(2).split(/\s+/).map((v, i) => Number(v) * scale / 16 + (i === 1 ? 0 : 0.5)).join(' ')
    : line.startsWith('usemtl ') ? 'usemtl refill_crystal_empty' : line).join('\n');
  fs.writeFileSync(assets + '/models/block/refill_crystal_empty.obj', obj + '\n');
  Project.save_path = art + '/refill_crystal_empty.bbmodel';
  fs.writeFileSync(Project.save_path, JSON.stringify(Codecs.project.compile({raw: true, bitmaps: true}), null, 2) + '\n');
  Project.saved = true;
  return {path: Project.save_path, mesh: mesh.uuid, faces: Object.keys(mesh.faces).length, texture: texture.uuid};
})()
