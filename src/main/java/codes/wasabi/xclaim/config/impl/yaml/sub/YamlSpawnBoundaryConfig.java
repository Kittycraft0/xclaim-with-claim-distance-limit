package codes.wasabi.xclaim.config.impl.yaml.sub;

import codes.wasabi.xclaim.config.impl.yaml.YamlConfig;
import codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public class YamlSpawnBoundaryConfig extends YamlConfig implements SpawnBoundaryConfig {

    public YamlSpawnBoundaryConfig(@NotNull ConfigurationSection section) {
        super(section);
    }

    @Override
    public @UnknownNullability Boolean enabled() {
        return this.getBoolean("enabled");
    }

    @Override
    public @UnknownNullability String insideName() {
        return this.getString("inside-name");
    }

    @Override
    public @UnknownNullability String enterMessage() {
        return this.getString("enter-message");
    }

    @Override
    public @UnknownNullability String leaveMessage() {
        return this.getString("leave-message");
    }

}