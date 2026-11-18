/* Run after create_model.js. texture.pixel.json is the editable pixel source. */
(() => {
  const fs = require('fs');
  const root = 'D:/MCMOD/no_ultra_dash_please';
  const asset = root + '/src/main/resources/assets/no_ultra_dash_please';
  if (Project?.name !== 'refill_crystal' || Format.id !== 'free' || Mesh.all.length !== 1 ||
      Mesh.all[0].name !== 'refill_crystal_octahedron' || Texture.all.length) {
    throw new Error('Expected the untextured refill crystal mesh only.');
  }
  const art = JSON.parse(fs.readFileSync(root + '/art/refill_crystal/texture.pixel.json', 'utf8'));
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 16;
  const ctx = canvas.getContext('2d');
  art.rows.forEach((row, y) => {
    if (row.length !== 16) throw new Error('Each pixel row must contain 16 characters.');
    [...row].forEach((pixel, x) => {ctx.fillStyle = art.palette[pixel]; ctx.fillRect(x, y, 1, 1);});
  });
  const texturePath = asset + '/textures/block/refill_crystal.png';
  fs.mkdirSync(asset + '/textures/block', {recursive: true});
  fs.writeFileSync(texturePath, Buffer.from(canvas.toDataURL().split(',')[1], 'base64'));
  Undo.initEdit({textures: [], elements: [Mesh.all[0]]});
  const texture = new Texture({name: 'refill_crystal.png', namespace: 'no_ultra_dash_please',
    folder: 'block', uv_width: 16, uv_height: 16}).fromDataURL(canvas.toDataURL()).add();
  texture.path = texturePath;
  texture.saved = true;
  Object.values(Mesh.all[0].faces).forEach(face => {face.texture = texture.uuid;});
  Canvas.updateAll();
  Undo.finishEdit('Paint original 16x16 Celeste-inspired crystal', {textures: [texture], elements: [Mesh.all[0]]});
  return {texture: texture.uuid, size: [16, 16]};
})()
