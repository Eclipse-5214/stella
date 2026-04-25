# Stella — Improvement Todo List

Ordered by impact.

---

## Critical / High Impact

- [ ] **1. Try-catch in EventBus dispatch**
  At 30k downloads, someone's machine will do something unexpected and a handler will throw. Right now that silently kills the entire event loop for that frame. One try-catch with a logger call protects every feature at once.
  *Why it matters: a single bad handler currently has blast radius across all handlers in that event.*

- [ ] **2. AstrumLayers line cache key collision**
  `width.hashCode()` as a `HashMap<Int, RenderType>` key means two different `Double` widths that happen to hash the same will silently share a render type, producing wrong line widths. Change the map to `HashMap<Double, RenderType>` and key on the value directly.
  *Why it matters: hash collisions are silent and produce visually wrong output that's very hard to debug.*

- [ ] **3. KSP typealias crash**
  The `as KSClassDeclaration` unsafe cast in `FeatureProcessor.kt` throws `ClassCastException` on any typealias in the codebase. Change both `.forEach` blocks to `.filterIsInstance<KSClassDeclaration>().forEach`.
  *Why it matters: it's a landmine — works until the day someone adds a typealias anywhere in the project.*

- [ ] **4. KSP `Dependencies.ALL_FILES`**
  The generated registry is invalidated and fully regenerated on every single file change, even unrelated ones. Scope it to only the files containing `@Module`/`@Command` annotations.
  *Why it matters: incremental build performance — right now every save triggers a full KSP pass.*

---

## Medium Impact

- [ ] **5. Secrets fetch jitter**
  All 5 party members fire secrets requests on the same dungeon event at the same millisecond. Add a small random delay (0–2s) before the initial fetch so the first client's request has time to populate the CF cache before the others fire.
  *Why it matters: reduces your Hypixel API key rate limit burn by up to 4x per dungeon run.*

- [ ] **6. Client-side in-flight deduplication in HypixelApi**
  If something triggers two fetches for the same UUID close together on a single client, both hit the network. A `HashMap<UUID, Deferred<*>>` tracking in-flight requests short-circuits the duplicate.
  *Why it matters: defensive hygiene — prevents accidental double-requests from edge cases in dungeon logic.*

- [ ] **7. `TEXT_SCALE_DISTANCE` constant in Astrum**
  `120f` in the distance-scaled text calculation is a magic number. Name it.
  *Why it matters: readability — your own stated goal. Future-you will not remember what 120 means.*

- [ ] **8. Player skin cache cleanup in ConfigUI**
  `imageCacheMap` only ever grows. Player skin NVG textures accumulate in VRAM and are never freed. Call `nvg.deleteImage()` for entries when the screen closes.
  *Why it matters: VRAM leak — small now, but grows with every unique player seen.*

- [ ] **9. Feature init failure visibility**
  If a feature's `initialize()` throws, it silently never registers. At minimum log the exception with the feature class name so you know which feature failed and why.
  *Why it matters: silent failures are the hardest bugs to diagnose — you can't fix what you can't see.*

---

## Lower Impact / Polish

- [ ] **10. `textWidth()` caching in TextHandler**
  Called twice per frame every frame. Cache the result and only recompute when the text, font, or size changes.
  *Why it matters: minor CPU saving, but it's the correct pattern for any measurement that's expensive relative to how often the value changes.*

- [ ] **11. `configListeners` thread safety**
  `ConcurrentHashMap<String, MutableList<Feature>>` — the map is thread-safe but `MutableList` isn't. Concurrent reads while a feature registers could cause a `ConcurrentModificationException`.
  *Why it matters: rare but non-deterministic crash that only shows up under specific timing conditions.*

- [ ] **12. `println("DEBUG: Merging...")` in WorldScanner**
  Left-in debug print. Replace with your logger or remove.
  *Why it matters: spams stdout for every merge event, which in a busy dungeon is a lot.*

- [ ] **13. Cloudflare Worker in-flight coalescing**
  On the server side — add a `Map<string, Promise<Response>>` in `globalThis` to coalesce concurrent requests for the same UUID within the same CF isolate.
  *Why it matters: the client-side jitter helps but server-side coalescing is the complete fix.*

---

## Nice To Have

- [ ] **14. `UI_SCALE` resolution independence in ConfigUI**
  Currently `windowWidth / 1920f` assumes a 1920px baseline. Ultrawide monitors, Steam Deck, and non-standard aspect ratios will look stretched.
  *Why it matters: you have 30k downloads — you have users on weird hardware.*

- [ ] **15. `beginFrame()` exception safety in NVGRenderer**
  The `drawing` flag has no `finally` block. If rendering throws, `drawing` gets stuck `true` and all subsequent NVG rendering breaks until restart.
  *Why it matters: rare but catastrophic when it happens — the symptom looks completely unrelated to the cause.*

- [ ] **16. Scroll momentum in ConfigUI**
  The scroll snaps directly to the new offset. iOS-style deceleration would feel significantly better.
  *Why it matters: pure polish — won't fix bugs but makes the UI feel more alive.*

- [ ] **17. Batched NVG rendering**
  Each `drawNVG` call produces one `nvgEndFrame` — a full GPU flush. With health and mana bars each calling `drawNVG` independently, you're flushing twice per frame minimum. Add a `batchNVG` utility that collects multiple render lambdas with their pose matrix snapshots and issues a single `NVGPIPRenderer.draw()`.
  *Why it matters: `nvgEndFrame` was the top entry in your Spark profiler — this is the highest-confidence performance win in the mod.*
