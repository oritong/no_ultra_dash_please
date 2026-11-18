#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Intensity;
uniform float Phase;

in vec2 texCoord;
out vec4 fragColor;

float hash(float seed) {
    return fract(sin(seed * 127.1 + 311.7) * 43758.5453);
}

vec2 safeUV(vec2 uv) {
    return clamp(uv, 0.5 / InSize, 1.0 - 0.5 / InSize);
}

void main() {
    float frame = floor(Phase * 45.0);
    vec2 shake = vec2(hash(frame + 7.0), hash(frame + 29.0)) * 2.0 - 1.0;
    shake *= vec2(0.006, 0.0035) * Intensity;

    // Slight overscan covers the edges during shake instead of exposing black strips.
    vec2 uv = (texCoord - 0.5) / (1.0 + 0.022 * Intensity) + 0.5 + shake;
    float band = floor(texCoord.y * 38.0);
    float tear = step(0.88, hash(band + frame * 19.0));
    uv.x += (hash(band * 3.1 + frame) * 2.0 - 1.0) * tear * 0.026 * Intensity;

    vec2 fromCenter = texCoord - 0.5;
    vec2 split = vec2((0.0025 + abs(fromCenter.x) * 0.005) * Intensity, 0.0);
    vec3 color;
    color.r = texture(DiffuseSampler, safeUV(uv + split)).r;
    color.g = texture(DiffuseSampler, safeUV(uv)).g;
    color.b = texture(DiffuseSampler, safeUV(uv - split)).b;

    // Fine scanlines, restrained cyan/magenta interference and a quick edge pulse.
    float scan = sin(texCoord.y * InSize.y * 3.14159265 + frame * 1.7);
    color *= 1.0 - (0.018 + 0.018 * scan) * Intensity;
    color += vec3(0.012, 0.025, 0.035) * tear * Intensity;
    float edge = smoothstep(0.18, 0.7, length(fromCenter));
    color += mix(vec3(0.0, 0.025, 0.04), vec3(0.03, 0.0, 0.025), step(0.5, hash(frame)))
            * edge * Intensity;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
