# Port Progress

## Phase 13: Minecraft 26.3

Status: Complete

### Scope

- Move the Fabric build and mod metadata from Minecraft 26.2 to final Minecraft 26.3.
- Pin the matching Fabric API and current compatible Loader and Loom versions.
- Update the installation and build documentation to describe the 26.3 target.
- Check source compilation, automated tests, and packaging with Java 25.
- Launch the dedicated server to check mod initialization and mixin application on 26.3.

### Acceptance criteria

- Gradle resolves Minecraft 26.3 with fixed 26.3 Fabric dependencies and no Loom snapshot.
- `fabric.mod.json`, Modrinth metadata, and README version references agree on Minecraft 26.3.
- `compileJava`, `test`, and `build` succeed with Java 25.
- The dedicated server reaches world startup without a mod or mixin error; gameplay with connected players remains unverified.

### Starting evidence

- The working tree was clean at `d6f6512` (`main`, matching `origin/main`).
- The current build targeted Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.158.0+26.2, and Loom 1.17-SNAPSHOT.
- Fabric Maven metadata lists Fabric API 0.161.0+26.3 and Loader 0.19.5; the Fabric Loom plugin marker metadata lists stable 1.17.21.
- The remote `v1.0.0` tag, GitHub release, and Modrinth version `1.0.0` identify the Minecraft 26.2 release, so this port uses the distinct candidate version `1.0.1-rc.1`.

### Completion evidence

- Updated the build pins to Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, and Fabric Loom 1.17.21.
- Set the mod candidate version to `1.0.1-rc.1` so it cannot collide with the published 26.2 `1.0.0` artifact.
- Updated `fabric.mod.json`, Modrinth's target-version comment, and the README badges and installation/build requirements.
- `.\gradlew.bat clean build --stacktrace` succeeded on Java 25. This ran `compileJava`, `test`, `jar`, and `build`.
- `runServer` loaded Minecraft 26.3, Fabric Loader 0.19.5, and Modern Player Ladder 1.0.0; the mod initialized and the server reached `Done` with no Mixin error. This verifies startup only, not player interaction or ladder behavior.
- The server was stopped. A follow-up check found no listener on port 25565 and no server process.
- The Loom task used the existing project `run/` directory, bound on `*:25565`, and wrote standard server files there. It used the newly named `ladder-26.3-smoke` save; the pre-existing `run/world` directory was not the selected save. The server files and logs under `run/` were preserved.
- Temporary paths created for this check were `run/server/` (the unused isolated eula/properties directory) and `run/ladder-26.3-smoke/` (the generated test world). A cleanup attempt was rejected by the execution policy, so both remain. No pre-existing run data was removed.
- Existing Windows performance-counter warnings and the JOML `Unsafe` warning appeared during startup/tests; neither stopped the build or server startup.
