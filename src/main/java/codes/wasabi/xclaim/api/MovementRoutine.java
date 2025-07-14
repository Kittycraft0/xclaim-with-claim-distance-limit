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

        // Only run logic if the player has actually moved to a new chunk
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()) {
            return;
        }

        Claim fromClaim = Claim.getByChunk(fromChunk);
        Claim toClaim = Claim.getByChunk(toChunk);

        // If the claim status hasn't changed, do nothing
        if (Objects.equals(fromClaim, toClaim)) {
            return;
        }

        Player ply = event.getPlayer();

        if (toClaim != null) {
            // Player has entered a new claim
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
            // Player has left a claim and entered an unclaimed chunk (since toClaim is null and fromClaim was not)
            Platform.get().sendActionBar(ply, XClaim.lang.getComponent(
                    "move-exit",
                    fromClaim.getName()
            ));
        }
    }

}