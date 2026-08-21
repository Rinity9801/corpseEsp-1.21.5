#version 330

uniform sampler2D OutlineSampler;
uniform sampler2D HaloSampler;

in vec2 texCoord;

out vec4 fragColor;

const float INTENSITY = 2.5;

void main() {
    vec4 outline = texture(OutlineSampler, texCoord);
    vec4 halo = texture(HaloSampler, texCoord);
    fragColor = vec4(
        outline.rgb * outline.a + halo.rgb * halo.a * INTENSITY,
        outline.a);
}
