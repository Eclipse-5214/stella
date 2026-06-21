#version 450

layout(set = 0, binding = 0) uniform sampler2D uTex;

layout(push_constant) uniform PushConstants {
    layout(offset = 64) int uMode;
};

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;

layout(location = 0) out vec4 fragColor;

void main() {
    if (uMode == 0) {
        float a = texture(uTex, vUV).r;
        fragColor = vColor * a;
    } else {
        fragColor = texture(uTex, vUV) * vColor;
    }
}
