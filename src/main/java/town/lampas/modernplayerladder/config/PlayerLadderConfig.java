package town.lampas.modernplayerladder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PlayerLadderConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("modern_player_ladder/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "modern-player-ladder.json";

    public static final List<String> DEFAULT_EXCLUDED_LIVING_ENTITIES = List.of(
        "minecraft:wither",
        "minecraft:ender_dragon",
        "minecraft:minecart",
        "#minecraft:boat",
        "#minecraft:dismounts_underwater"
    );

    public static final ClickMode DEFAULT_RIGHT_CLICK_MODE = ClickMode.RIDE;
    public static final int DEFAULT_PICK_UP_LIMIT = 16;
    public static final int DEFAULT_STEP_UP_LIMIT = 16;
    public static final boolean DEFAULT_ALLOW_LIVING_ENTITIES = false;
    public static final boolean DEFAULT_ALLOW_PLAYERS = true;
    public static final boolean DEFAULT_RIDE_COMMAND_EXTENSION = true;
    public static final boolean DEFAULT_ALLOW_INTERACTIONS = true;

    private static PlayerLadderConfig currentConfig = createDefault();

    private static final Set<EntityType<?>> excludedEntityTypes = new HashSet<>();
    private static final Set<TagKey<EntityType<?>>> excludedEntityTags = new HashSet<>();

    private final ClickMode rightClickMode;
    private final int pickUpLimit;
    private final int stepUpLimit;
    private final boolean allowLivingEntities;
    private final boolean allowPlayers;
    private final List<String> excludedLivingEntities;
    private final boolean rideCommandExtension;
    private final boolean allowInteractions;

    public PlayerLadderConfig(
        ClickMode rightClickMode,
        int pickUpLimit,
        int stepUpLimit,
        boolean allowLivingEntities,
        boolean allowPlayers,
        List<String> excludedLivingEntities,
        boolean rideCommandExtension,
        boolean allowInteractions
    ) {
        this.rightClickMode = rightClickMode != null ? rightClickMode : DEFAULT_RIGHT_CLICK_MODE;
        this.pickUpLimit = pickUpLimit >= 1 ? pickUpLimit : DEFAULT_PICK_UP_LIMIT;
        this.stepUpLimit = stepUpLimit >= 1 ? stepUpLimit : DEFAULT_STEP_UP_LIMIT;
        this.allowLivingEntities = allowLivingEntities;
        this.allowPlayers = allowPlayers;
        this.excludedLivingEntities = excludedLivingEntities != null ? List.copyOf(excludedLivingEntities) : DEFAULT_EXCLUDED_LIVING_ENTITIES;
        this.rideCommandExtension = rideCommandExtension;
        this.allowInteractions = allowInteractions;
    }

    public static PlayerLadderConfig createDefault() {
        return new PlayerLadderConfig(
            DEFAULT_RIGHT_CLICK_MODE,
            DEFAULT_PICK_UP_LIMIT,
            DEFAULT_STEP_UP_LIMIT,
            DEFAULT_ALLOW_LIVING_ENTITIES,
            DEFAULT_ALLOW_PLAYERS,
            DEFAULT_EXCLUDED_LIVING_ENTITIES,
            DEFAULT_RIDE_COMMAND_EXTENSION,
            DEFAULT_ALLOW_INTERACTIONS
        );
    }

    public static PlayerLadderConfig get() {
        return currentConfig;
    }

    public static void setInstance(PlayerLadderConfig config) {
        currentConfig = config != null ? config : createDefault();
        rebuildEntityExclusions();
    }

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve(CONFIG_FILE_NAME);
        load(configFile);
    }

    public static void load(Path configFile) {
        if (!Files.exists(configFile)) {
            LOGGER.info("Config file {} not found. Creating with defaults.", configFile.getFileName());
            PlayerLadderConfig defaultConfig = createDefault();
            save(configFile, defaultConfig);
            setInstance(defaultConfig);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            PlayerLadderConfig loaded = parseJson(reader);
            setInstance(loaded);
            LOGGER.info("Loaded configuration from {}", configFile.getFileName());
        } catch (Exception e) {
            LOGGER.warn("Failed to load configuration from {}. Using defaults. Error: {}", configFile.getFileName(), e.getMessage());
            setInstance(createDefault());
        }
    }

    public static void save(Path configFile, PlayerLadderConfig config) {
        try {
            if (configFile.getParent() != null) {
                Files.createDirectories(configFile.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration to {}: {}", configFile, e.getMessage());
        }
    }

    public static PlayerLadderConfig parseJson(BufferedReader reader) {
        JsonObject obj;
        try {
            JsonElement elem = JsonParser.parseReader(reader);
            if (elem == null || !elem.isJsonObject()) {
                LOGGER.warn("Config JSON root is not an object. Falling back to defaults.");
                return createDefault();
            }
            obj = elem.getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("Malformed config JSON: {}. Falling back to defaults.", e.getMessage());
            return createDefault();
        }
        return fromJsonObject(obj);
    }

    public static PlayerLadderConfig parseJson(String json) {
        JsonObject obj;
        try {
            JsonElement elem = JsonParser.parseString(json);
            if (elem == null || !elem.isJsonObject()) {
                LOGGER.warn("Config JSON root is not an object. Falling back to defaults.");
                return createDefault();
            }
            obj = elem.getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warn("Malformed config JSON: {}. Falling back to defaults.", e.getMessage());
            return createDefault();
        }
        return fromJsonObject(obj);
    }

    public static PlayerLadderConfig fromJsonObject(JsonObject obj) {
        ClickMode mode = DEFAULT_RIGHT_CLICK_MODE;
        if (obj.has("rightClickMode")) {
            try {
                mode = ClickMode.valueOf(obj.get("rightClickMode").getAsString().toUpperCase());
            } catch (Exception e) {
                LOGGER.warn("Invalid rightClickMode value in config: {}. Defaulting to {}", obj.get("rightClickMode"), DEFAULT_RIGHT_CLICK_MODE);
            }
        }

        int pickUpLimit = DEFAULT_PICK_UP_LIMIT;
        if (obj.has("pickUpLimit")) {
            try {
                pickUpLimit = obj.get("pickUpLimit").getAsInt();
                if (pickUpLimit < 1) {
                    LOGGER.warn("pickUpLimit {} must be >= 1. Defaulting to {}", pickUpLimit, DEFAULT_PICK_UP_LIMIT);
                    pickUpLimit = DEFAULT_PICK_UP_LIMIT;
                }
            } catch (Exception e) {
                LOGGER.warn("Invalid pickUpLimit in config. Defaulting to {}", DEFAULT_PICK_UP_LIMIT);
            }
        }

        int stepUpLimit = DEFAULT_STEP_UP_LIMIT;
        if (obj.has("stepUpLimit")) {
            try {
                stepUpLimit = obj.get("stepUpLimit").getAsInt();
                if (stepUpLimit < 1) {
                    LOGGER.warn("stepUpLimit {} must be >= 1. Defaulting to {}", stepUpLimit, DEFAULT_STEP_UP_LIMIT);
                    stepUpLimit = DEFAULT_STEP_UP_LIMIT;
                }
            } catch (Exception e) {
                LOGGER.warn("Invalid stepUpLimit in config. Defaulting to {}", DEFAULT_STEP_UP_LIMIT);
            }
        }

        boolean allowLivingEntities = DEFAULT_ALLOW_LIVING_ENTITIES;
        if (obj.has("allowLivingEntities")) {
            try {
                allowLivingEntities = obj.get("allowLivingEntities").getAsBoolean();
            } catch (Exception e) {
                LOGGER.warn("Invalid allowLivingEntities in config. Defaulting to {}", DEFAULT_ALLOW_LIVING_ENTITIES);
            }
        }

        boolean allowPlayers = DEFAULT_ALLOW_PLAYERS;
        if (obj.has("allowPlayers")) {
            try {
                allowPlayers = obj.get("allowPlayers").getAsBoolean();
            } catch (Exception e) {
                LOGGER.warn("Invalid allowPlayers in config. Defaulting to {}", DEFAULT_ALLOW_PLAYERS);
            }
        }

        List<String> excludedLivingEntities = new ArrayList<>();
        if (obj.has("excludedLivingEntities") && obj.get("excludedLivingEntities").isJsonArray()) {
            for (JsonElement elem : obj.getAsJsonArray("excludedLivingEntities")) {
                if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                    excludedLivingEntities.add(elem.getAsString());
                }
            }
        } else {
            excludedLivingEntities = DEFAULT_EXCLUDED_LIVING_ENTITIES;
        }

        boolean rideCommandExtension = DEFAULT_RIDE_COMMAND_EXTENSION;
        if (obj.has("rideCommandExtension")) {
            try {
                rideCommandExtension = obj.get("rideCommandExtension").getAsBoolean();
            } catch (Exception e) {
                LOGGER.warn("Invalid rideCommandExtension in config. Defaulting to {}", DEFAULT_RIDE_COMMAND_EXTENSION);
            }
        }

        boolean allowInteractions = DEFAULT_ALLOW_INTERACTIONS;
        if (obj.has("allowInteractions")) {
            try {
                allowInteractions = obj.get("allowInteractions").getAsBoolean();
            } catch (Exception e) {
                LOGGER.warn("Invalid allowInteractions in config. Defaulting to {}", DEFAULT_ALLOW_INTERACTIONS);
            }
        }

        return new PlayerLadderConfig(
            mode,
            pickUpLimit,
            stepUpLimit,
            allowLivingEntities,
            allowPlayers,
            excludedLivingEntities,
            rideCommandExtension,
            allowInteractions
        );
    }

    public static void rebuildEntityExclusions() {
        excludedEntityTypes.clear();
        excludedEntityTags.clear();

        try {
            for (String entry : currentConfig.excludedLivingEntities()) {
                if (entry == null || entry.isBlank()) continue;

                try {
                    if (entry.startsWith("#")) {
                        String tagPath = entry.substring(1);
                        Identifier tagId = Identifier.tryParse(tagPath);
                        if (tagId != null) {
                            excludedEntityTags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                        } else {
                            LOGGER.warn("Malformed entity tag in config: {}", entry);
                        }
                    } else {
                        Identifier entityId = Identifier.tryParse(entry);
                        if (entityId != null) {
                            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
                            if (type.isPresent()) {
                                excludedEntityTypes.add(type.get());
                            } else {
                                LOGGER.debug("Entity type not found in registry (yet): {}", entry);
                            }
                        } else {
                            LOGGER.warn("Malformed entity ID in config: {}", entry);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error parsing excluded living entity entry '{}': {}", entry, e.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not rebuild entity exclusions (registries may not be initialized): {}", t.getMessage());
        }
    }

    public static boolean isEntityExcluded(EntityType<?> type) {
        if (excludedEntityTypes.contains(type)) {
            return true;
        }
        for (TagKey<EntityType<?>> tag : excludedEntityTags) {
            if (net.fabricmc.fabric.api.tag.convention.v2.TagUtil.isIn(tag, type)) {
                return true;
            }
        }
        return false;
    }

    public static Set<EntityType<?>> getExcludedEntityTypes() {
        return Collections.unmodifiableSet(excludedEntityTypes);
    }

    public static Set<TagKey<EntityType<?>>> getExcludedEntityTags() {
        return Collections.unmodifiableSet(excludedEntityTags);
    }

    public ClickMode rightClickMode() { return rightClickMode; }
    public int pickUpLimit() { return pickUpLimit; }
    public int stepUpLimit() { return stepUpLimit; }
    public boolean allowLivingEntities() { return allowLivingEntities; }
    public boolean allowPlayers() { return allowPlayers; }
    public List<String> excludedLivingEntities() { return excludedLivingEntities; }
    public boolean rideCommandExtension() { return rideCommandExtension; }
    public boolean allowInteractions() { return allowInteractions; }
}
