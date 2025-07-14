package codes.wasabi.xclaim.config.impl.defaulting.sub;

import codes.wasabi.xclaim.config.impl.filter.sub.FilterRulesConfig;
import codes.wasabi.xclaim.config.struct.sub.RulesConfig;
import codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public final class DefaultingRulesConfig extends FilterRulesConfig {

    private final SpawnBoundaryConfig spawnBoundary;
    public DefaultingRulesConfig(@NotNull RulesConfig backing) {
        super(backing);
        this.spawnBoundary = new DefaultingSpawnBoundaryConfig(backing.spawnBoundary());
    }

    @Override
    public @NotNull Integer placementRaw() {
        return this.nullFallback(this.backing().placementRaw(), PlacementRule.NEIGHBOR.code());
    }

    @Override
    public @NotNull PlacementRule placement() {
        return this.nullFallback(this.backing().placement(), PlacementRule.NEIGHBOR);
    }

    @Override
    public @NotNull Integer minDistance() {
        return this.nullFallback(this.backing().minDistance(), 0);
    }

    @Override
    public @NotNull Boolean exemptOwner() {
        return this.nullFallback(this.backing().exemptOwner(), true);
    }

    @Override
    public @NotNull Integer maxChunks(@Nullable Permissible target) {
        return this.nullFallback(this.backing().maxChunks(target), 20);
    }

    @Override
    public @NotNull Integer maxClaims(@Nullable Permissible target) {
        return this.nullFallback(this.backing().maxClaims(target), 5);
    }

    @Override
    public @NotNull Integer maxClaimsInWorld(@Nullable Permissible target) {
        return this.nullFallback(this.backing().maxClaimsInWorld(target), -1);
    }

    @Override
    public @NotNull Integer spawnClaimRadius() {
        return this.nullFallback(this.backing().spawnClaimRadius(), 0);
    }

    @Override
    public @NotNull Boolean exemptOpsFromSpawnRestriction() {
        return this.nullFallback(this.backing().exemptOpsFromSpawnRestriction(), true);
    }

    @Override
    public @NotNull Boolean useSpawnRestrictionWhitelist() {
        return this.nullFallback(this.backing().useSpawnRestrictionWhitelist(), false);
    }

    @Override
    public @NotNull List<String> spawnRestrictionWhitelist() {
        List<String> value = this.backing().spawnRestrictionWhitelist();
        if (value == null) return Collections.emptyList();
        return value;
    }

    @Override
    public @NotNull SpawnBoundaryConfig spawnBoundary() {
        return this.spawnBoundary;
    }

}