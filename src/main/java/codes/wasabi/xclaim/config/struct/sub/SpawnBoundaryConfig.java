package codes.wasabi.xclaim.config.struct.sub;

import codes.wasabi.xclaim.config.struct.Config;
import org.jetbrains.annotations.UnknownNullability;

public interface SpawnBoundaryConfig extends Config {

    @UnknownNullability Boolean enabled();

    @UnknownNullability String insideName();

    @UnknownNullability String enterMessage();

    @UnknownNullability String leaveMessage();

}