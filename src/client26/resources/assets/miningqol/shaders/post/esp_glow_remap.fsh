#version 330

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float OUTER = 0.7;

void main() {
    vec2 texel = 1.0 / InSize;
    vec4 acc = vec4(0.0);
    float weightSum = 0.0;

    for (int x = -3; x <= 3; x++) {
        for (int y = -3; y <= 3; y++) {
            float weight = exp(-float(x * x + y * y) / 6.0);
            acc += weight * texture(InSampler, texCoord + vec2(float(x), float(y)) * texel);
            weightSum += weight;
        }
    }
    acc /= weightSum;

    float alpha = clamp(acc.a, 0.0, 1.0);
    float halo = alpha * (1.0 - alpha) * 4.0;
    float strength = clamp(halo * OUTER, 0.0, 1.0);
    vec3 color = alpha > 0.003 ? acc.rgb / alpha : vec3(0.0);
    fragColor = vec4(color, strength);
}
