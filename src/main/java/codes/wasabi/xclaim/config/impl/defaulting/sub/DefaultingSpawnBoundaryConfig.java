package codes.wasabi.xclaim.config.impl.defaulting.sub;

import codes.wasabi.xclaim.config.impl.filter.sub.FilterSpawnBoundaryConfig;
import codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig;
import org.jetbrains.annotations.NotNull;

public class DefaultingSpawnBoundaryConfig extends FilterSpawnBoundaryConfig {

    public DefaultingSpawnBoundaryConfig(@NotNull SpawnBoundaryConfig backing) {
        super(backing);
    }

    @Override
    public @NotNull Boolean enabled() {
        return this.nullFallback(this.backing().enabled(), true);
    }

    @Override
    public @NotNull String insideName() {
        return this.nullFallback(this.backing().insideName(), "Spawn");
    }

    @Override
    public @NotNull String enterMessage() {
        return this.nullFallback(this.backing().enterMessage(), "<gray>Entering <green>$1</green></gray>");
    }

    @Override
    public @NotNull String leaveMessage() {
        return this.nullFallback(this.backing().leaveMessage(), "<gray>Leaving <red>$1</red></gray>");
    }

}