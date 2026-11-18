#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Intensity;
uniform float Progress;
uniform float DashSeed;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.28318531;
const vec3 CORE = vec3(1.0, 0.97, 0.88);
const vec3 CYAN = vec3(0.18, 0.94, 1.0);
const vec3 MAGENTA = vec3(1.0, 0.20, 0.72);
const vec3 VIOLET = vec3(0.38, 0.25, 1.0);

float hash(float value) {
    return fract(sin(value * 127.1 + DashSeed * 17.17) * 43758.5453);
}

vec2 safeUV(vec2 uv) {
    return clamp(uv, 0.5 / InSize, 1.0 - 0.5 / InSize);
}

vec3 spectrum(int tap) {
    if (tap == 0) return vec3(0.05, 0.18, 1.0);
    if (tap == 1) return vec3(0.04, 0.60, 1.0);
    if (tap == 2) return vec3(0.10, 1.00, 0.75);
    if (tap == 3) return vec3(0.65, 1.00, 0.70);
    if (tap == 4) return vec3(1.00, 0.80, 0.35);
    if (tap == 5) return vec3(1.00, 0.28, 0.45);
    return vec3(1.00, 0.08, 0.50);
}

float thinLine(float distanceToLine, float halfWidth, float aa) {
    return 1.0 - smoothstep(max(0.0, halfWidth - aa), halfWidth + aa, distanceToLine);
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 aspect = vec2(InSize.x / InSize.y, 1.0);
    vec2 point = (texCoord - 0.5) * aspect;
    float radius = length(point);
    float t = clamp(Progress, 0.0, 1.0);

    // Exact scene passthrough in the central 0.42-height circle, at 0%, and after dissolve.
    if (radius <= 0.42 || Intensity <= 0.0 || t >= 1.0) {
        fragColor = original;
        return;
    }

    vec2 radial = point / radius;
    vec2 tangent = vec2(-radial.y, radial.x);
    float rayEdge = min(0.5 * aspect.x / max(abs(radial.x), 0.0001),
                        0.5 / max(abs(radial.y), 0.0001));
    float rimPosition = radius / rayEdge;
    float safeMask = smoothstep(0.42, 0.50, radius);
    float attack = 1.0 - pow(1.0 - min(t / 0.12, 1.0), 4.0);
    float erosion = clamp((t - 0.12) / 0.88, 0.0, 1.0);
    float power = Intensity * attack;
    float pixel = 1.0 / InSize.y;
    float angle = atan(point.y, point.x);

    // Layer 2: triangular prisms made from straight half-planes, clipped to the outer rim.
    float sectorWidth = TAU / 22.0;
    float sector = floor((angle + PI) / sectorWidth);
    float sectorAngle = (sector + 0.5) * sectorWidth - PI;
    vec2 axis = vec2(cos(sectorAngle), sin(sectorAngle));
    vec2 acrossAxis = vec2(-axis.y, axis.x);
    float along = dot(point, axis);
    float across = dot(point, acrossAxis);
    float sectorEdge = min(0.5 * aspect.x / max(abs(axis.x), 0.0001),
                           0.5 / max(abs(axis.y), 0.0001));
    float facetRandom = hash(sector * 11.7 + 9.0);
    float apex = sectorEdge * (0.61 + facetRandom * 0.12 + erosion * 0.40);
    float slope = 0.28 + hash(sector + 29.0) * 0.30;
    float sideDistance = abs(across) - (along - apex) * slope;
    float facetAA = max(fwidth(sideDistance), pixel);
    float facet = (1.0 - smoothstep(-facetAA, facetAA, sideDistance))
            * step(apex, along) * step(0.30, facetRandom);
    float outerRing = smoothstep(0.72 + erosion * 0.27, 0.87 + erosion * 0.12, rimPosition);
    facet *= outerRing * safeMask;
    float facetEdge = thinLine(abs(sideDistance), pixel * 0.65, facetAA * 0.45)
            * step(apex, along) * step(0.30, facetRandom) * outerRing * safeMask;
    vec3 facetColor = mix(CYAN, MAGENTA, step(0.5, hash(sector * 3.7)));
    vec2 prismOffset = (radial * (hash(sector + 5.0) - 0.5)
            + tangent * (hash(sector + 16.0) - 0.5) * 0.55)
            * facet * power * 0.009 * pow(1.0 - erosion, 1.5);

    // Layer 1: seven spectral samples along the radius; normalization avoids a rainbow wash.
    float distortion = power * safeMask * pow(rimPosition, 2.0) * pow(1.0 - erosion, 1.8);
    vec2 zoom = (texCoord - 0.5) / (1.0 + 0.018 * distortion) + 0.5;
    vec2 uv = zoom + prismOffset / aspect;
    vec3 refracted = vec3(0.0), weights = vec3(0.0);
    for (int tap = 0; tap < 7; tap++) {
        float offset = (float(tap) - 3.0) / 3.0;
        vec3 weight = spectrum(tap);
        refracted += texture(DiffuseSampler, safeUV(uv + radial / aspect * offset * 0.005 * distortion)).rgb * weight;
        weights += weight;
    }
    vec3 color = mix(original.rgb, refracted / weights, safeMask);

    // Layer 3: uneven clusters of finite light needles, tapered at both ends and eroded outward.
    float slots = 112.0;
    float angular = (angle + PI) / TAU * slots;
    float slot = floor(angular);
    float lineRandom = hash(slot * 9.13 + 61.0);
    float cluster = step(0.28, hash(floor(slot / 4.0) * 7.1));
    float enabled = step(0.42, lineRandom) * cluster;
    float lineAngle = (slot + 0.15 + hash(slot + 71.0) * 0.70) / slots * TAU - PI;
    float angularDistance = abs(atan(sin(angle - lineAngle), cos(angle - lineAngle))) * radius;
    float start = rayEdge * (0.43 + 0.26 * hash(slot + 42.0) + erosion * 0.64);
    start = max(start, 0.445 + erosion * 0.12);
    float length = rayEdge * (0.13 + 0.30 * hash(slot + 21.0)) * (1.0 - 0.62 * erosion);
    float lineProgress = (radius - start) / max(length, 0.001);
    float taper = pow(max(0.0, min(lineProgress, 1.0 - lineProgress) * 2.0), 0.65);
    float needle = step(0.68, hash(slot + 38.0));
    float width = mix(0.0038, 0.0011, needle) * (0.55 + lineRandom)
            * taper * pow(1.0 - erosion, 1.15);
    float lineAA = pixel * 0.55;
    float body = thinLine(angularDistance, width, lineAA) * step(0.0, lineProgress) * step(lineProgress, 1.0);
    float core = thinLine(angularDistance, width * 0.22, lineAA * 0.55)
            * step(0.0, lineProgress) * step(lineProgress, 1.0);
    float fragment = step(erosion * 0.85, hash(slot + floor(lineProgress * 4.0) * 13.0 + 100.0));
    float hue = clamp(lineProgress, 0.0, 1.0);
    vec3 trail = hue < 0.5 ? mix(CYAN, VIOLET, hue * 2.0) : mix(VIOLET, MAGENTA, hue * 2.0 - 1.0);
    vec3 energy = (trail * body * 0.40 + CORE * core * 1.25)
            * enabled * fragment * safeMask * power;
    energy += (facetColor * 0.24 + CORE * 0.42) * facetEdge * power * (1.0 - erosion);

    // Layer 4: very short edge contrast pulse. No global tint, blur, shake or scanlines.
    float burst = (1.0 - smoothstep(0.04, 0.15, t)) * attack * safeMask * Intensity;
    color *= 1.0 - 0.055 * burst * smoothstep(0.65, 1.0, rimPosition);
    energy += CORE * 0.018 * burst * smoothstep(0.78, 1.0, rimPosition);
    color = pow(max(vec3(0.0), pow(max(color, vec3(0.0)), vec3(2.2)) + energy), vec3(1.0 / 2.2));
    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
