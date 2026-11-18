/* Inspect the empty mesh and compare the two saved crystal projects offscreen. */
(async () => {
  const root = 'D:/MCMOD/no_ultra_dash_please', fs = require('fs');
  const empty = Project;
  if (empty?.name !== 'refill_crystal_empty' || Mesh.all.length !== 1) throw Error('Select the empty crystal.');
  const filled = ModelProject.all.find(p => p.name === 'refill_crystal' && p.save_path.replaceAll('\\', '/') === root + '/art/refill_crystal/refill_crystal.bbmodel');
  if (!filled) throw Error('Open the filled crystal project for the comparison.');
  const view = Preview.all.find(p => p.id === 'mcp_offscreen_empty_crystal_inspection');
  if (!view) throw Error('Create the empty_crystal_inspection offscreen view first.');
  const dir = root + '/build/art-validation/empty';
  fs.mkdirSync(dir, {recursive: true});
  const capture = position => {
    view.setProjectionMode(true);
    view.camOrtho.position.set(...position);
    view.controls.target.set(0, 8, 0);
    view.controls.update();
    Object.assign(view.camOrtho, {left: -8, right: 8, top: 8, bottom: -8, zoom: 1, near: -1000, far: 1000});
    view.camOrtho.updateProjectionMatrix();
    const material = Mesh.all[0].mesh.material, side = material.side;
    material.side = THREE.FrontSide; material.needsUpdate = true;
    let url;
    try { Canvas.withoutGizmos(() => {view.render(); url = view.canvas.toDataURL();}); }
    finally {material.side = side; material.needsUpdate = true;}
    return url;
  };
  const cameras = {perspective: [-28, 24, -36], front: [0, 8, -40], back: [0, 8, 40],
    left: [-40, 8, 0], right: [40, 8, 0], top: [0, 48, -0.001], bottom: [0, -32, -0.001]};
  const images = {};
  for (const [name, position] of Object.entries(cameras)) {
    images[name] = capture(position);
    fs.writeFileSync(dir + '/' + name + '.png', Buffer.from(images[name].split(',')[1], 'base64'));
  }
  let filledImage;
  try {filled.select(); filledImage = capture(cameras.perspective);}
  finally {empty.select();}
  const load = src => new Promise((resolve, reject) => {const img = new Image(); img.onload = () => resolve(img); img.onerror = reject; img.src = src;});
  const board = document.createElement('canvas'); board.width = 1100; board.height = 850;
  const ctx = board.getContext('2d');
  ctx.fillStyle = '#16232c'; ctx.fillRect(0, 0, 1100, 850);
  ctx.fillStyle = '#e7f3ed'; ctx.font = 'bold 28px sans-serif'; ctx.fillText('DASH REFILL  /  CHARGED + EMPTY', 32, 48);
  ctx.fillStyle = '#a6beb5'; ctx.font = '15px sans-serif';
  ctx.fillText('Same octahedron  /  white interrupted outline  /  transparent center  /  native 16 x 16', 32, 79);
  ctx.drawImage(await load(filledImage), 6, 100, 440, 440);
  ctx.drawImage(await load(images.perspective), 434, 100, 440, 440);
  ctx.font = 'bold 17px sans-serif'; ctx.fillStyle = '#adf27c'; ctx.fillText('CHARGED', 163, 553);
  ctx.fillStyle = '#ffffff'; ctx.fillText('EMPTY', 616, 553);
  ctx.imageSmoothingEnabled = false;
  ctx.drawImage(Texture.all[0].img, 918, 170, 128, 128);
  ctx.drawImage(Texture.all[0].img, 973, 340, 16, 16);
  ctx.font = '14px sans-serif'; ctx.fillStyle = '#a6beb5';
  ctx.fillText('16 x 16', 940, 389); ctx.fillText('White + clear', 926, 419); ctx.fillText('12 white pixels', 920, 449);
  const labels = ['front', 'back', 'left', 'right', 'top', 'bottom'];
  for (const [i, name] of labels.entries()) {
    const x = 29 + i * 176, y = 589;
    ctx.fillStyle = '#25343f'; ctx.fillRect(x, y, 160, 170);
    ctx.drawImage(await load(images[name]), x, y, 160, 160);
    ctx.fillStyle = '#c2d2ca'; ctx.font = '13px sans-serif'; ctx.fillText(name.toUpperCase(), x + 12, y + 192);
  }
  ctx.font = '13px sans-serif'; ctx.fillStyle = '#8da79b';
  ctx.fillText('Blockbench 5.2.2 / Front-face culling / 1 mesh, 8 triangles, 1 cutout material / Minecraft client untested', 32, 828);
  const path = root + '/art/refill_crystal_empty/preview.png';
  fs.writeFileSync(path, Buffer.from(board.toDataURL().split(',')[1], 'base64'));
  return {preview: path, views: Object.keys(images)};
})()
