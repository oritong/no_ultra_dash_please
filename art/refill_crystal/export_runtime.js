/* Export the active refill crystal through Blockbench's native OBJ codec. */
(() => {
  const fs = require('fs'), root = 'D:/MCMOD/no_ultra_dash_please';
  if (Project?.name !== 'refill_crystal' || Format.id !== 'free' || Mesh.all.length !== 1 ||
      Mesh.all[0].name !== 'refill_crystal_octahedron' || Object.keys(Mesh.all[0].faces).length !== 8) {
    throw new Error('Select the refill_crystal Generic Model project.');
  }
  const scale = Settings.get('model_export_scale');
  let obj = Codecs.obj.compile({mtl_name: 'refill_crystal.mtl'});
  // BB Generic is centered at X/Z=0, Minecraft OBJ uses the block corner at 0.
  obj = obj.split('\n').map(line => line.startsWith('v ')
    ? 'v ' + line.slice(2).split(/\s+/).map((v, i) => Number(v) * scale / 16 + (i === 1 ? 0 : 0.5)).join(' ')
    : line.startsWith('usemtl ') ? 'usemtl refill_crystal' : line).join('\n');
  fs.writeFileSync(root + '/src/main/resources/assets/no_ultra_dash_please/models/block/refill_crystal.obj', obj + '\n');
  Project.save_path = root + '/art/refill_crystal/refill_crystal.bbmodel';
  // The normal BB JSON formatter rounds coordinates. Preserve analytical geometry.
  fs.writeFileSync(Project.save_path, JSON.stringify(Codecs.project.compile({raw: true, bitmaps: true}), null, 2) + '\n');
  Project.saved = true;
  return {model: Project.save_path, exportedFaces: obj.split('\n').filter(line => line.startsWith('f ')).length};
})()
