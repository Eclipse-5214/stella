#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

// Renamed to match the source fragment shader input
out vec2 f_Position;

void main() {
    f_Position = Position.xy;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}