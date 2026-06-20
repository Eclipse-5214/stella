#version 330 core
in vec4 vColor;
in float vCoverage;
out vec4 fragColor;
void main() {
    fragColor = vColor * vCoverage;
}
