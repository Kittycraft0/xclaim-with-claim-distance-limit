package codes.wasabi.xclaim.config.impl.toml.sub;

import codes.wasabi.xclaim.config.impl.toml.TomlConfig;
import codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig;
import com.moandjiezana.toml.Toml;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class TomlSpawnBoundaryConfig extends TomlConfig implements SpawnBoundaryConfig {

    public TomlSpawnBoundaryConfig(@Nullable Toml table) {
        super(table);
    }

    @Override
    public @UnknownNullability Boolean enabled() {
        return getBoolean("enabled");
    }

    @Override
    public @UnknownNullability String insideName() {
        return getString("inside-name");
    }

    @Override
    public @UnknownNullability String enterMessage() {
        return getString("enter-message");
    }

    @Override
    public @UnknownNullability String leaveMessage() {
        return getString("leave-message");
    }

}