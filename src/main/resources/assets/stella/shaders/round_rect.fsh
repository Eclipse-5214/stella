#version 150

#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform u {
    vec4 u_Rect;
    vec4 u_Radii;
    vec4 u_colorRect;
    vec4 u_colorRect2;
    vec4 u_colorShadow;
    vec2 u_gradientDirectionVector;
    float u_edgeSoftness;
    float u_shadowSoftness;
};

#define u_rectCenter u_Rect.xy
#define u_rectSize u_Rect.zw

in vec2 f_Position;
out vec4 fragColor;

float roundedBoxSDF(vec2 CenterPosition, vec2 Size, vec4 Radius) {
    Radius.xy = (CenterPosition.x > 0.) ? Radius.xy : Radius.zw;
    Radius.x  = (CenterPosition.y > 0.) ? Radius.x  : Radius.y;

    vec2 q = abs(CenterPosition) - Size + Radius.x;
    return min(max(q.x, q.y), 0.) + length(max(q, 0.)) - Radius.x;
}

void main() {
    vec2 uv = (f_Position - u_rectCenter) / u_rectSize;
    // The source logic for the gradient direction
    float gradientStrength = clamp(dot(uv, u_gradientDirectionVector) + .5, 0., 1.);
    vec4 gradientColor = mix(u_colorRect, u_colorRect2, gradientStrength);

    vec2 halfSize = (u_rectSize / 2.);

    float distance = roundedBoxSDF(f_Position - u_rectCenter, halfSize, u_Radii);
    float smoothedAlpha = 1. - smoothstep(0., u_edgeSoftness, distance);
    float shadowAlpha = 1. - smoothstep(-u_shadowSoftness, u_shadowSoftness, distance);

    // Optimized: If it's fully transparent, don't waste GPU cycles
    //if (max(smoothedAlpha, shadowAlpha) <= 0.0) discard;

    vec4 resShadowColor = mix(
    vec4(0.),
    vec4(u_colorShadow.rgb, u_colorShadow.a * shadowAlpha),
    shadowAlpha
    );

    fragColor = mix(resShadowColor, gradientColor, smoothedAlpha);
}