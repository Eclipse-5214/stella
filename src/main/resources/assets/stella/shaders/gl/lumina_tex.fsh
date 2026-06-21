#version 330 core
uniform sampler2D uTex;
uniform int uMode;
in vec2 vUV;
in vec4 vColor;
out vec4 fragColor;
void main() {
    if (uMode == 0) {
        float a = texture(uTex, vUV).r;
        fragColor = vColor * a;
    } else {
        fragColor = texture(uTex, vUV) * vColor;
    }
}
