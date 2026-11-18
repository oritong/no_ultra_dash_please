/* Offscreen, reversible preview inspection; does not change the saved model. */
(async () => {
  if (Project?.name !== 'refill_crystal' || Mesh.all.length !== 1) throw Error('Wrong project');
  const fs = require('fs'), root = 'D:/MCMOD/no_ultra_dash_please';
  const view = Preview.all.find(p => p.id === 'mcp_offscreen_refill_inspection');
  if (!view) throw Error('Create refill_inspection offscreen view first');
  const mesh = Mesh.all[0].mesh, material = mesh.material, side = material.side;
  const out = root + '/build/art-validation';
  fs.mkdirSync(out, {recursive: true});
  view.setProjectionMode(true);
  Object.assign(view.camOrtho, {left: -8, right: 8, top: 8, bottom: -8, zoom: 1, near: -1000, far: 1000});
  view.camOrtho.updateProjectionMatrix();
  material.side = THREE.FrontSide;
  material.needsUpdate = true;
  const images = {};
  const cameras = {
    perspective: [-28, 24, -36], front: [0, 8, -40], back: [0, 8, 40],
    left: [-40, 8, 0], right: [40, 8, 0], top: [0, 48, -0.001], bottom: [0, -32, -0.001]
  };
  try {
    for (const [name, position] of Object.entries(cameras)) {
      view.camOrtho.position.set(...position);
      view.controls.target.set(0, 8, 0);
      view.controls.update();
      Canvas.withoutGizmos(() => {
        view.render();
        images[name] = view.canvas.toDataURL();
        fs.writeFileSync(out + '/' + name + '.png', Buffer.from(images[name].split(',')[1], 'base64'));
      });
    }
    const board = document.createElement('canvas');
    board.width = 1080; board.height = 700;
    const ctx = board.getContext('2d');
    ctx.fillStyle = '#141e27'; ctx.fillRect(0, 0, 1080, 700);
    ctx.fillStyle = '#e4f5e9'; ctx.font = 'bold 28px sans-serif'; ctx.fillText('DASH REFILL CRYSTAL', 32, 48);
    ctx.fillStyle = '#94b3a3'; ctx.font = '15px sans-serif';
    ctx.fillText('Regular octahedron  /  6 vertices  /  8 triangular faces  /  native 16 x 16 texture', 32, 78);
    const load = src => new Promise((resolve, reject) => {const img = new Image(); img.onload = () => resolve(img); img.onerror = reject; img.src = src;});
    ctx.drawImage(await load(images.perspective), 8, 94, 510, 510);
    ctx.fillStyle = '#94b3a3'; ctx.fillText('BLOCKBENCH 5.2.2  /  FRONT-FACE CULLING', 32, 645);
    for (const [i, name] of ['front', 'back', 'left', 'right', 'top', 'bottom'].entries()) {
      const x = 528 + i % 3 * 172, y = 108 + Math.floor(i / 3) * 206;
      ctx.fillStyle = '#1c2a33'; ctx.fillRect(x, y, 160, 170);
      ctx.drawImage(await load(images[name]), x, y, 160, 160);
      ctx.fillStyle = '#b4cbbf'; ctx.font = '13px sans-serif'; ctx.fillText(name.toUpperCase(), x + 12, y + 188);
    }
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(Texture.all[0].img, 548, 550, 96, 96);
    ctx.drawImage(Texture.all[0].img, 674, 588, 16, 16);
    ctx.fillStyle = '#b4cbbf'; ctx.font = '14px sans-serif';
    ctx.fillText('16 x 16', 720, 581);
    ctx.fillText('7 colors / 1 material / 1 frame', 720, 607);
    ctx.fillText('Original pixel art, Celeste reference', 720, 633);
    fs.writeFileSync(root + '/art/refill_crystal/preview.png', Buffer.from(board.toDataURL().split(',')[1], 'base64'));
    return {views: Object.keys(images), preview: root + '/art/refill_crystal/preview.png'};
  } finally {
    material.side = side;
    material.needsUpdate = true;
  }
})()
