# Porting features from 1.21.x (Yarn) to 26.1.2 (unobfuscated Mojang names)

Minecraft 26.1.2 ships unobfuscated, so the Yarn-mapped shared client code
(`src/client/`) cannot compile against it. 26.1.2 uses a **twin-source** setup:
each feature is re-written natively in `src/client26/` (Mojang names, new APIs).
Stonecutter comments (`//? if ...`) do NOT work in this tree — files are synced
verbatim into the build.

Source layout:
- `src/client26/java/` — Java, both cheat and legit 26.1.2 variants
- `src/client26cheat/java/` — Java compiled ONLY into the 26.1.2-cheat variant
- `src/client26/kotlin/` — Kotlin (Vexel GUI screens), joint-compiled with the java dir
- 1.21 reference sources: `src/client/java/`, `src/client/kotlin/` (READ ONLY —
  never import from there; translate)

## Verify, don't guess

Every Minecraft symbol you are not 100% sure of must be checked against the real jar:

```bash
MC="/Users/alexdong/Library/Application Support/PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar"
javap -cp "$MC" net.minecraft.client.Minecraft | grep -i someMethod
unzip -l "$MC" | grep -i SomeClass        # find where a class moved
```

Compile loop (from repo root, safe to run repeatedly):
```bash
./gradlew :26.1.2-cheat:compileClientJava :26.1.2-cheat:compileClientKotlin
```

## Verified rename table (Yarn 1.21.x → 26.1.2)

| 1.21.x (Yarn) | 26.1.2 |
|---|---|
| `MinecraftClient.getInstance()` | `net.minecraft.client.Minecraft.getInstance()` |
| `client.world` | `client.level` (`ClientLevel`) |
| `client.player` (sendMessage(text,false)) | `client.player.sendSystemMessage(Component)` |
| `Text.literal(...)` | `net.minecraft.network.chat.Component.literal(...)` |
| `player.getBlockPos()` | `player.blockPosition()` |
| `pos.down()/up()/add(x,y,z)` | `pos.below()/above()/offset(x,y,z)` (`net.minecraft.core.BlockPos`) |
| `pos.getSquaredDistance(other)` | `pos.distSqr(other)` |
| `Registries.BLOCK.get(Identifier.of(s))` | `BuiltInRegistries.BLOCK.getValue(Identifier.parse(s))` (`net.minecraft.core.registries`, `net.minecraft.resources.Identifier` — yes, it's called Identifier here) |
| `world.getChunkManager().isChunkLoaded(cx,cz)` | `client.level.hasChunk(cx, cz)` |
| `client.keyboard.getClipboard()` | `client.keyboardHandler.getClipboard()` |
| `client.textRenderer` | `client.font` (`net.minecraft.client.gui.Font`) |
| `client.send(runnable)` | `client.schedule(runnable)` (next task drain, render thread) |
| `MatrixStack` | `com.mojang.blaze3d.vertex.PoseStack` (push/popPose, mulPose) |
| `Vec3d` | `net.minecraft.world.phys.Vec3` |
| `KeyBindingHelper` (key-binding-api) | `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper` (`KeyMapping` class) |
| `ClientCommandManager` (command-api-v2) | `net.fabricmc.fabric.api.client.command.v2.ClientCommands` (same literal/argument API) |
| `ClientTickEvents` | unchanged: `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents` |

Chat color codes: use `§` escapes in string literals, never a raw `§`.

## Rendering — the old world is gone

`RenderLayer`/`RenderLayers` + raw GL11 state calls DO NOT EXIST on 26.1.2.

**World rendering** (boxes/lines/labels in the world): hook is already built —
`forfun.miningqol.mixin.client.LevelRendererMixin` injects at TAIL of
`LevelRenderer.renderLevel` and calls into renderers with
`(CameraRenderState cameraState, Matrix4fc viewMatrix)`. Follow
`client26/.../waypoints/OrderedWaypointRenderer.java` as the canonical example:
- buffer source: `Minecraft.getInstance().renderBuffers().bufferSource()`
- camera position: `cameraState.pos` (Vec3); orientation: `cameraState.orientation`
- render types live in `net.minecraft.client.renderer.rendertype.RenderTypes`
- see-through (through-wall) colored quads: `RenderTypes.textBackgroundSeeThrough()`
  with `addVertex(matrix,x,y,z).setColor(r,g,b,a).setLight(15728880)`
- depth-tested lines: `RenderTypes.lines()` with `.setColor().setNormal().setLineWidth(2f)`
- world text: `client.font.drawInBatch(text, x, y, argb, shadow, matrix, buffers,
  Font.DisplayMode.SEE_THROUGH, 0x40000000, 15728880)`
- vertices are camera-relative; multiply through the passed view matrix
- finish with `buffers.endBatch()`
- new world renderers: add a call in `LevelRendererMixin`, gate everything on
  your feature's enabled flag first

**HUD rendering** (2D overlay): register via
`net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(id, (context, tickCounter) -> ...)`
— follow `client26/.../CommissionHUD.java` (uses `GuiGraphicsExtractor`,
`RenderPipelines.GUI_TEXTURED`).

## Screens (Vexel)

Vexel screens are Kotlin in `src/client26/kotlin/.../gui/`. The 26 Vexel API
differs from 1.21: `Pos`/`Size` are in `xyz.meowing.vexel.components.base.enums`
(`Size.Percent`, not `ParentPerc`); there are `Switch` and `Slider` elements.
Use the `SettingsUi` helpers + copy `OrderedWaypointsCategoryScreen.kt`.
- NEVER open a screen with Vexel's `display()` — it fires from a timer thread and
  crashes fabric-screen-api. Use `client.schedule(() -> client.setScreen(...))`.
- Add new feature settings as a category card in `gui/VexelMainScreen.kt`.

## Integration points (coordinate — single owner each)

- `MiningqolClient.java` (entrypoint): feature init, tick handlers, command registration
- `config/MiningConfig.java`: fields + `applyToGame()` + `loadFromGame()`
- `miningqol.client.mixins.json`: register any new mixin class
- `gui/VexelMainScreen.kt`: category cards

When porting a feature as a standalone class, keep the same public static API
(setEnabled/isEnabled/getters/setters) as the 1.21 version so config and GUI
wiring stays mechanical.
