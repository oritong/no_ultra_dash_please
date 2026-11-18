/* Offscreen previews. The neutral mannequin is temporary render geometry, never exported. */
(async () => {
  const fs = require('fs'), root = 'D:/MCMOD/no_ultra_dash_please';
  const names = ['booster', 'booster_occupied', 'red_booster', 'red_booster_occupied'];
  const projects = names.map(name => ModelProject.all.find(p => p.name === name && p.save_path.replaceAll('\\', '/') === root + '/art/boosters/models/' + name + '.bbmodel'));
  if (projects.some(p => !p || !p.saved)) throw Error('Open and save all four booster projects first.');
  const view = Preview.all.find(p => p.id === 'mcp_offscreen_booster_inspection');
  if (!view) throw Error('Create the booster_inspection offscreen view first.');
  const start = Project;
  const out = root + '/build/art-validation/boosters';
  fs.mkdirSync(out, {recursive: true});
  const images = {};
  const cameras = {front: [0, 0, -60], back: [0, 0, 60], left: [-60, 0, 0],
    right: [60, 0, 0], top: [0, 60, -0.001], bottom: [0, -60, -0.001], perspective: [-45, 24, -60]};
  const capture = (name, position, center, range) => {
    view.setProjectionMode(true);
    view.camOrtho.position.set(position[0], position[1] + center, position[2]);
    view.controls.target.set(0, center, 0); view.controls.update();
    Object.assign(view.camOrtho, {left: -range, right: range, top: range, bottom: -range, zoom: 1, near: -1000, far: 1000});
    view.camOrtho.updateProjectionMatrix();
    let url;
    Canvas.withoutGizmos(() => {view.render(); url = view.canvas.toDataURL();});
    fs.writeFileSync(out + '/' + name + '.png', Buffer.from(url.split(',')[1], 'base64'));
    return url;
  };
  try {
    for (let index = 0; index < projects.length; index++) {
      projects[index].select();
      const occupied = names[index].endsWith('_occupied'), center = occupied ? 18 : 8;
      for (const [angle, camera] of Object.entries(cameras)) images[names[index] + '_' + angle] =
        capture(names[index] + '_' + angle, camera, center, occupied ? 23 : 10);
      if (occupied) {
        const mannequin = new THREE.Group();
        mannequin.position.y = 3.6; mannequin.scale.setScalar(0.9);
        const add = (size, pos, color) => {
          const m = new THREE.Mesh(new THREE.BoxGeometry(...size), new THREE.MeshBasicMaterial({color}));
          m.position.set(...pos); mannequin.add(m);
        };
        add([8, 8, 8], [0, 28, 0], 0xc7bea5);
        add([8, 12, 4], [0, 18, 0], 0x31414b);
        add([4, 12, 4], [-6, 18, 0], 0x687875); add([4, 12, 4], [6, 18, 0], 0x687875);
        add([4, 12, 4], [-2, 6, 0], 0x29363c); add([4, 12, 4], [2, 6, 0], 0x29363c);
        Project.model_3d.add(mannequin);
        try {images[names[index] + '_player'] = capture(names[index] + '_player', cameras.perspective, center, 23);}
        finally {mannequin.removeFromParent(); mannequin.children.forEach(m => {m.geometry.dispose(); m.material.dispose();});}
      }
    }
  } finally {start.select();}
  const load = src => new Promise((resolve, reject) => {const img = new Image(); img.onload = () => resolve(img); img.onerror = reject; img.src = src;});
  const board = document.createElement('canvas'); board.width = 1200; board.height = 780;
  const ctx = board.getContext('2d'); ctx.fillStyle = '#182730'; ctx.fillRect(0, 0, 1200, 780);
  ctx.fillStyle = '#e0f4e8'; ctx.font = 'bold 28px sans-serif'; ctx.fillText('CELESTE BOOSTERS  /  IDLE + PLAYER WRAP', 30, 48);
  ctx.font = '15px sans-serif'; ctx.fillStyle = '#9fbeb1';
  ctx.fillText('Native 16 x 16 pixels / Source color ramps / 42 vertices, 48 faces per shell / Neutral mannequin for scale', 30, 78);
  for (const [i, name] of names.entries()) {
    const x = i * 300;
    ctx.fillStyle = '#20323c'; ctx.fillRect(x + 12, 104, 276, 380);
    const key = name + (name.endsWith('_occupied') ? '_player' : '_perspective');
    ctx.drawImage(await load(images[key]), x + 8, 142, 284, 284);
    ctx.fillStyle = i < 2 ? '#8cf7cf' : '#ffc494'; ctx.font = 'bold 17px sans-serif';
    ctx.fillText(name.toUpperCase(), x + 26, 515);
    for (const [j, angle] of ['front', 'left', 'top'].entries()) {
      ctx.drawImage(await load(images[name + '_' + angle]), x + 4 + j * 98, 553, 94, 94);
    }
  }
  ctx.fillStyle = '#a3c1b2'; ctx.font = '14px sans-serif';
  ctx.fillText('Blockbench screenshots / Occupied shell uses alpha, front faces only / Minecraft runtime not tested', 30, 741);
  fs.writeFileSync(root + '/art/boosters/preview.png', Buffer.from(board.toDataURL().split(',')[1], 'base64'));
  return {preview: root + '/art/boosters/preview.png', cameraViewsPerState: 7, playerPreviews: 2};
})()
