package town.lampas.modernplayerladder.config;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerLadderConfigTest {

    @BeforeAll
    static void setup() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testDefaultConfig() {
        PlayerLadderConfig config = PlayerLadderConfig.createDefault();
        assertEquals(ClickMode.RIDE, config.rightClickMode());
        assertEquals(16, config.pickUpLimit());
        assertEquals(16, config.stepUpLimit());
        assertFalse(config.allowLivingEntities());
        assertTrue(config.allowPlayers());
        assertEquals(5, config.excludedLivingEntities().size());
        assertTrue(config.excludedLivingEntities().contains("minecraft:wither"));
        assertTrue(config.excludedLivingEntities().contains("#minecraft:boat"));
        assertTrue(config.rideCommandExtension());
        assertTrue(config.allowInteractions());
    }

    @Test
    void testParseValidJson() {
        String json = """
            {
              "rightClickMode": "PICK_UP",
              "pickUpLimit": 8,
              "stepUpLimit": 12,
              "allowLivingEntities": true,
              "allowPlayers": false,
              "excludedLivingEntities": [
                "minecraft:pig",
                "#minecraft:dismounts_underwater"
              ],
              "rideCommandExtension": false,
              "allowInteractions": false
            }
            """;

        PlayerLadderConfig config = PlayerLadderConfig.parseJson(json);
        assertEquals(ClickMode.PICK_UP, config.rightClickMode());
        assertEquals(8, config.pickUpLimit());
        assertEquals(12, config.stepUpLimit());
        assertTrue(config.allowLivingEntities());
        assertFalse(config.allowPlayers());
        assertEquals(List.of("minecraft:pig", "#minecraft:dismounts_underwater"), config.excludedLivingEntities());
        assertFalse(config.rideCommandExtension());
        assertFalse(config.allowInteractions());
    }

    @Test
    void testMalformedJsonFallsBackToDefaults() {
        String invalidJson = "{ this is not valid json: true ";
        PlayerLadderConfig config = PlayerLadderConfig.parseJson(invalidJson);
        assertNotNull(config);
        assertEquals(ClickMode.RIDE, config.rightClickMode());
        assertEquals(16, config.pickUpLimit());
        assertEquals(16, config.stepUpLimit());
    }

    @Test
    void testNonObjectJsonFallsBackToDefaults() {
        String arrayJson = "[\"not\", \"an\", \"object\"]";
        PlayerLadderConfig config = PlayerLadderConfig.parseJson(arrayJson);
        assertNotNull(config);
        assertEquals(ClickMode.RIDE, config.rightClickMode());
    }

    @Test
    void testInvalidValuesFallBackToDefaults() {
        String json = """
            {
              "rightClickMode": "INVALID_MODE",
              "pickUpLimit": -5,
              "stepUpLimit": 0
            }
            """;

        PlayerLadderConfig config = PlayerLadderConfig.parseJson(json);
        assertEquals(ClickMode.RIDE, config.rightClickMode());
        assertEquals(16, config.pickUpLimit());
        assertEquals(16, config.stepUpLimit());
    }

    @Test
    void testSaveAndLoadFile(@TempDir Path tempDir) {
        Path configFile = tempDir.resolve("modern-player-ladder.json");
        PlayerLadderConfig original = new PlayerLadderConfig(
            ClickMode.DO_NOTHING,
            32,
            64,
            true,
            true,
            List.of("minecraft:creeper"),
            false,
            true
        );

        PlayerLadderConfig.save(configFile, original);
        assertTrue(configFile.toFile().exists());

        PlayerLadderConfig.load(configFile);
        PlayerLadderConfig loaded = PlayerLadderConfig.get();

        assertEquals(ClickMode.DO_NOTHING, loaded.rightClickMode());
        assertEquals(32, loaded.pickUpLimit());
        assertEquals(64, loaded.stepUpLimit());
        assertTrue(loaded.allowLivingEntities());
        assertTrue(loaded.allowPlayers());
        assertEquals(List.of("minecraft:creeper"), loaded.excludedLivingEntities());
        assertFalse(loaded.rideCommandExtension());
        assertTrue(loaded.allowInteractions());
    }

    @Test
    void testEntityExclusions() {
        PlayerLadderConfig config = new PlayerLadderConfig(
            ClickMode.RIDE,
            16,
            16,
            true,
            true,
            List.of("minecraft:wither", "minecraft:invalid_entity_id_xyz", "#minecraft:boat", "#invalid:tag@!"),
            true,
            true
        );

        PlayerLadderConfig.setInstance(config);

        EntityType<?> wither = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:wither"));
        EntityType<?> pig = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:pig"));
        EntityType<?> boat = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:boat"));

        assertTrue(PlayerLadderConfig.isEntityExcluded(wither));
        assertFalse(PlayerLadderConfig.isEntityExcluded(pig));
        assertTrue(PlayerLadderConfig.getExcludedEntityTags().contains(
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.parse("minecraft:boat"))
        ));
    }
}
