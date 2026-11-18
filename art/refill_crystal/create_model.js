/* Run in Blockbench's JavaScript context on the empty refill_crystal project. */
(() => {
  if (!Project || Project.name !== 'refill_crystal' || Format.id !== 'free' || Outliner.elements.length) {
    throw new Error('Select the empty refill_crystal Generic Model project first.');
  }
  const radius = 4 * Math.SQRT2;
  const mesh = new Mesh({name: 'refill_crystal_octahedron', vertices: {}, shading: 'flat', origin: [0, 0, 0]});
  Undo.initEdit({elements: [], outliner: true});
  const keys = mesh.addVertices(
    [0, 8 + radius, 0], [0, 8 - radius, 0],
    [0, 8, -radius], [radius, 8, 0], [0, 8, radius], [-radius, 8, 0]
  );
  const [top, bottom, ...ring] = keys;
  for (let i = 0; i < 4; i++) {
    const a = ring[i], b = ring[(i + 1) % 4];
    mesh.addFaces(new MeshFace(mesh, {vertices: [top, b, a], uv: {
      [top]: [8, 8 - 4 * Math.sqrt(3)], [b]: [12, 8], [a]: [4, 8]
    }}));
    mesh.addFaces(new MeshFace(mesh, {vertices: [bottom, a, b], uv: {
      [bottom]: [8, 8 + 4 * Math.sqrt(3)], [a]: [4, 8], [b]: [12, 8]
    }}));
  }
  mesh.init();
  Project.texture_width = Project.texture_height = 16;
  Undo.finishEdit('Create six-vertex eight-face refill crystal', {elements: [mesh], outliner: true});
  Canvas.updateAll();
  return {uuid: mesh.uuid, vertices: Object.keys(mesh.vertices).length, faces: Object.keys(mesh.faces).length};
})()
