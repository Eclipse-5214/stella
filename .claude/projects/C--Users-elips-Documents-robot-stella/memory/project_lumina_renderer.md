---
name: lumina-renderer-plan
description: Lumina renderer replacing NVG — GL backend now, Vulkan backend for 26.2+. SDF-through-PIP approach failed due to PIP compositor not handling partial alpha.
metadata:
  type: project
---

Lumina is a custom 2D renderer replacing NanoVG. The API surface (Lumina object) stays identical so call sites don't change.

**Why:** NVG is GL-only and won't survive MC's GL→Vulkan transition. Zero external dependencies goal.

**Architecture decision (2026-06-18):** SDF shader approach through MC's RenderPipeline/PIP system FAILED. The PIP compositor cannot handle partial alpha — only hard `discard` produces visible rounding. Root cause confirmed via debug shader: localPos/SDF math is correct, but alpha blending is lost in PIP compositing.

**Chosen approach:** Lightweight GL renderer using raw OpenGL calls (same technique as NVG). Tessellated geometry with fringe triangles for AA. Runs inside existing PIP callback. ~400 lines.

**Backends:**
- GL backend: for MC 26.1 (current). Uses raw LWJGL OpenGL calls inside PIP.
- Vulkan backend: for MC 26.2+. LWJGL Vulkan package available in 26.2. [[lumina-vulkan-backend]]

**How to apply:** When working on Lumina rendering, use raw GL/Vulkan calls — do NOT use MC's RenderPipeline system for shape rendering. The PIP system is only used as a timing hook.

**Remaining phases:** Text (STBTruetype), Images (STBImage), Masking, State management — all per RENDERER_PLAN.md.
