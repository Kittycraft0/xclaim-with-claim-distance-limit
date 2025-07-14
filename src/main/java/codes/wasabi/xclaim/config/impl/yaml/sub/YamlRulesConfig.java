package codes.wasabi.xclaim.config.impl.yaml.sub;

import codes.wasabi.xclaim.config.impl.yaml.YamlConfig;
import codes.wasabi.xclaim.config.impl.yaml.helpers.YamlLimits;
import codes.wasabi.xclaim.config.struct.sub.RulesConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import java.util.List;

public final class YamlRulesConfig extends YamlConfig implements RulesConfig {

    public YamlRulesConfig(@NotNull ConfigurationSection section, @NotNull YamlLimits limits) {
        super(section);
    }

    @Override
    public @UnknownNullability Integer placementRaw() {
        return this.getInt("placement");
    }

    @Override
    public @UnknownNullability Integer minDistance() {
        return this.getInt("min-distance");
    }

    @Override
    public @UnknownNullability Boolean exemptOwner() {
        return this.getBoolean("exempt-owner");
    }

    @Override
    public @UnknownNullability Integer maxChunks(@Nullable Permissible target) {
        // Corrected: Return null to allow DefaultingRulesConfig to handle it.
        return null;
    }

    @Override
    public @UnknownNullability Integer maxClaims(@Nullable Permissible target) {
        // Corrected: Return null to allow DefaultingRulesConfig to handle it.
        return null;
    }

    @Override
    public @UnknownNullability Integer maxClaimsInWorld(@Nullable Permissible target) {
        // Corrected: Return null to allow DefaultingRulesConfig to handle it.
        return null;
    }

    @Override
    public @UnknownNullability Integer spawnClaimRadius() {
        return this.getInt("spawn-claim-radius");
    }

    @Override
    public @UnknownNullability Boolean exemptOpsFromSpawnRestriction() {
        return this.getBoolean("exempt-ops-from-spawn-restriction");
    }

    @Override
    public @UnknownNullability Boolean useSpawnRestrictionWhitelist() {
        return this.getBoolean("use-spawn-restriction-whitelist");
    }

    @Override
    public @UnknownNullability List<String> spawnRestrictionWhitelist() {
        return this.section.getStringList("spawn-restriction-whitelist");
    }

}