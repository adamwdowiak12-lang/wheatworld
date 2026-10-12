# Wheat World (Fabric 1.21.11)

Vanilla terrain, but every overworld surface column becomes permanently wet farmland with fully grown wheat.

## Build
Requires JDK 21 and Gradle 8.14+ (9.x is fine). Easiest: copy gradlew, gradlew.bat and the gradle/wrapper folder
from the fabric-example-mod template, then:

    ./gradlew build

Jar: build/libs/wheatworld-1.0.0.jar. Put it in your mods folder with Fabric API 0.141.1+1.21.11.

## Dev run
    ./gradlew runClient

## Notes
- Chunks are converted nearest-first around each player once fully loaded (new and already-generated alike).
- Converted chunks are listed in <world>/wheatworld_processed.bin so player edits are never overwritten.
  Delete that file to re-convert everything.
- Underwater columns get farmland on the seabed only (wheat cannot exist in water).
- Structure tops (village roofs etc.) are treated like any other surface and get converted.
- Trees, plants and snow layers on the surface are removed.
