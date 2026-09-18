#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
