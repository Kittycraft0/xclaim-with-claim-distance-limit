package codes.wasabi.xclaim.api;

import codes.wasabi.xclaim.XClaim;
import codes.wasabi.xclaim.platform.Platform;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MovementRoutine implements Listener {

    private static MovementRoutine instance;
    private static boolean initialized = false;
    public static MovementRoutine getInstance() {
        return instance;
    }

    public static void initialize() {
        if (initialized) return;
        instance = new MovementRoutine();
        Bukkit.getPluginManager().registerEvents(instance, XClaim.instance);
        initialized = true;
    }

    public static void cleanup() {
        if (!initialized) return;
        HandlerList.unregisterAll(instance);
        instance = null;
        initialized = false;
    }

    private MovementRoutine() { }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        Chunk fromChunk = from.getChunk();
        Chunk toChunk = to.getChunk();

        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }

        Player ply = event.getPlayer();
        Claim fromClaim = Claim.getByChunk(fromChunk);
        Claim toClaim = Claim.getByChunk(toChunk);

        boolean claimMessageSent = false;
        if (!Objects.equals(fromClaim, toClaim)) {
            if (toClaim != null) {
                XCPlayer claimOwner = toClaim.getOwner();
                Component ownerName;
                String n = claimOwner.getName();
                if (n == null) n = XClaim.lang.get("unknown");
                ownerName = Component.text(n);

                Platform.get().sendActionBar(ply, XClaim.lang.getComponent(
                        "move-enter",
                        ownerName,
                        Component.text(toClaim.getName())
                ));
            } else {
                Platform.get().sendActionBar(ply, XClaim.lang.getComponent(
                        "move-exit",
                        fromClaim.getName()
                ));
            }
            claimMessageSent = true;
        }

        if (!claimMessageSent) {
            codes.wasabi.xclaim.config.struct.sub.SpawnBoundaryConfig boundaryConfig = XClaim.mainConfig.rules().spawnBoundary();
            if (boundaryConfig.enabled()) {
                boolean wasInside = !codes.wasabi.xclaim.gui.ChunkEditor.violatesSpawnBoundaryCheck(ply, fromChunk);
                boolean isInside = !codes.wasabi.xclaim.gui.ChunkEditor.violatesSpawnBoundaryCheck(ply, toChunk);

                String message = null;
                if (wasInside && !isInside) {
                    message = boundaryConfig.leaveMessage().replace("$1", boundaryConfig.insideName());
                } else if (!wasInside && isInside) {
                    // This line was corrected
                    message = boundaryConfig.enterMessage().replace("$1", boundaryConfig.insideName());
                }

                if (message != null) {
                    // This line was corrected to use the MiniMessage parser
                    Platform.get().sendActionBar(ply, XClaim.Lang.mm.deserialize(message));
                }
            }
        }
    }

}