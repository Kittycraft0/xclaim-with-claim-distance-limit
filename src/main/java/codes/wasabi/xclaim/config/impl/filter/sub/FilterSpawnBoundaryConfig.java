package codes.wasabi.xclaim.config.impl.filter.sub;

import codes.wasabi.xclaim.config.impl.filter.FilterConfig;
import codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig;
import org.jetbrains.annotations.NotNull;

public class FilterSpawnBoundaryConfig extends FilterConfig implements SpawnBoundaryConfig {

    private final SpawnBoundaryConfig backing;
    public FilterSpawnBoundaryConfig(@NotNull SpawnBoundaryConfig backing) {
        super(backing);
        this.backing = backing;
    }

    protected final @NotNull SpawnBoundaryConfig backing() {
        return this.backing;
    }

    @Override
    public Boolean enabled() {
        return this.backing.enabled();
    }

    @Override
    public String insideName() {
        return this.backing.insideName();
    }

    @Override
    public String enterMessage() {
        return this.backing.enterMessage();
    }

    @Override
    public String leaveMessage() {
        return this.backing.leaveMessage();
    }

}