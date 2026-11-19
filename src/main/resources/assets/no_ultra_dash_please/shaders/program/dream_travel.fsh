#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Amount;
uniform float TravelTime;
uniform float TravelX;
uniform float TravelY;
uniform float TravelZ;
in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

vec3 palette(float choice) {
    int index = int(floor(choice * 9.0));
    if (index == 0) return vec3(1.0, 0.937, 0.067);
    if (index == 1) return vec3(1.0, 0.0, 0.816);
    if (index == 2) return vec3(0.031, 0.639, 0.063);
    if (index == 3) return vec3(0.373, 0.804, 0.894);
    if (index == 4) return vec3(0.498, 0.698, 0.369);
    if (index == 5) return vec3(0.878, 0.337, 0.298);
    if (index == 6) return vec3(0.357, 0.431, 0.882);
    if (index == 7) return vec3(0.8, 0.231, 0.231);
    return vec3(0.490, 0.667, 0.392);
}

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    vec2 aspect = vec2(OutSize.x / OutSize.y, 1.0);
    // The entire world becomes opaque dream space, with the block's original palette.
    vec3 dream = vec3(0.002, 0.003, 0.008);
    vec2 sideTravel = vec2(TravelX, TravelY);
    vec2 center = vec2(0.5) + sideTravel * 0.22;
    vec2 p = (texCoord - center) * aspect;
    float pixel = 2.0 / OutSize.y;
    float forward = abs(TravelZ);
    for (int layer = 0; layer < 3; layer++) {
        float l = float(layer);
        float cycle = fract(TravelTime * 0.34 * -TravelZ + l / 3.0 + 0.13);
        float scale = exp2(cycle * 2.8 - 1.25);
        vec2 drift = sideTravel * TravelTime * (0.45 + l * 0.16);
        vec2 grid = (p / scale + drift) * 10.0;
        vec2 cell = floor(grid);
        float fade = smoothstep(0.0, 0.08, cycle) * (1.0 - smoothstep(0.86, 1.0, cycle));
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec2 id = cell + vec2(float(x), float(y));
                float seed = hash(id + l * 61.7);
                if (seed < 0.58) continue;
                vec2 point = id + 0.15 + vec2(hash(id + 17.3 + l), hash(id + 53.9 - l)) * 0.7;
                vec2 delta = (grid - point) / 10.0 * scale;
                vec2 star = floor(delta / pixel + 0.5);
                float twinkle = floor(mod(TravelTime * 6.0 + seed * 13.0, 4.0));
                float radius = layer == 0 ? 0.0 : (twinkle == 1.0 ? 2.0 : 1.0);
                float shape = step(abs(star.x) + abs(star.y), radius);
                if (layer == 2) {
                    float crossShape = max(step(abs(star.x), 0.0) * step(abs(star.y), 2.0),
                                           step(abs(star.y), 0.0) * step(abs(star.x), 2.0));
                    shape = max(shape, crossShape);
                }
                vec2 ray = normalize(p + sideTravel * 0.5 + vec2(0.0001));
                float along = dot(delta, ray);
                float across = abs(dot(delta, vec2(-ray.y, ray.x)));
                float tailLength = pixel * (2.0 + cycle * 10.0) * forward;
                float tail = (1.0 - smoothstep(pixel * 0.3, pixel * 0.65, across))
                           * step(-tailLength, along) * step(along, 0.0);
                vec3 color = palette(hash(id + 97.2 + l * 19.0));
                dream += color * (shape + tail * 0.32) * fade * (0.5 + l * 0.25);
            }
        }
    }
    fragColor = vec4(mix(scene, clamp(dream, 0.0, 1.0), Amount), 1.0);
}
