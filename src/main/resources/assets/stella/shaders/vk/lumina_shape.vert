#version 450

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec4 aColor;
layout(location = 2) in float aCoverage;

layout(push_constant) uniform PushConstants {
    mat4 uProjection;
};

layout(location = 0) out vec4 vColor;
layout(location = 1) out float vCoverage;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vColor = aColor;
    vCoverage = aCoverage;
}
