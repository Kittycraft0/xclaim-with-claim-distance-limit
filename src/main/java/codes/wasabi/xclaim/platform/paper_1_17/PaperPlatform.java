package codes.wasabi.xclaim.platform.paper_1_17;

import codes.wasabi.xclaim.platform.PlatformChatListener;
import codes.wasabi.xclaim.platform.spigot_1_17.SpigotPlatform_1_17;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PaperPlatform extends SpigotPlatform_1_17 {

    @Override
    public void sendActionBar(@NotNull Player ply, @NotNull Component text) {
        try {
            // This is the new, direct approach.
            // 1. Convert the plugin's message component into a legacy string with § color codes.
            String legacyText = LegacyComponentSerializer.legacySection().serialize(text);

            // 2. Use the Spigot API to send this text to the action bar.
            ply.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
        } catch (Exception e) {
            // Failsafe in case of any unexpected errors during the process
            e.printStackTrace();
        }
    }

    @Override
    public void setOwningPlayer(SkullMeta sm, UUID owner, String username) {
        sm.setPlayerProfile(Bukkit.createProfile(owner, username));
    }

    @Override
    public void setOwningPlayer(SkullMeta sm, OfflinePlayer player) {
        sm.setPlayerProfile(player.getPlayerProfile());
    }

    @Override
    protected PlatformChatListener newChatListener() {
        return new PaperPlatformChatListener();
    }

    @Override
    public @Nullable OfflinePlayer getOfflinePlayerIfCached(@NotNull String name) {
        return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public void closeInventory(@NotNull Inventory iv) {
        iv.close();
    }

    @Override
    public long getLastSeen(@NotNull OfflinePlayer ply) {
        return ply.getLastSeen();
    }

    @Override
    public @NotNull Location toCenterLocation(@NotNull Location loc) {
        return loc.toCenterLocation();
    }

    @Override
    public @Nullable Location getInteractionPoint(@NotNull PlayerInteractEvent event) {
        return event.getInteractionPoint();
    }

    @Override
    public boolean supportsArtificalElytraBoost() {
        return true;
    }

    @Override
    public void artificialElytraBoost(Player ply, ItemStack is) {
        ply.boostElytra(is);
    }

}