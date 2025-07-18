package codes.wasabi.xclaim.gui;

import codes.wasabi.xclaim.XClaim;
import codes.wasabi.xclaim.api.Claim;
import codes.wasabi.xclaim.api.XCPlayer;
import codes.wasabi.xclaim.api.enums.Permission;
import codes.wasabi.xclaim.api.event.XClaimAddChunkToClaimEvent;
import codes.wasabi.xclaim.api.event.XClaimEvent;
import codes.wasabi.xclaim.api.event.XClaimRemoveChunkFromClaimEvent;
import codes.wasabi.xclaim.config.struct.sub.RulesConfig;
import codes.wasabi.xclaim.economy.Economy;
import codes.wasabi.xclaim.particle.ParticleBuilder;
import codes.wasabi.xclaim.particle.ParticleEffect;
import codes.wasabi.xclaim.platform.*;
import codes.wasabi.xclaim.protection.ProtectionRegion;
import codes.wasabi.xclaim.protection.ProtectionService;
import codes.wasabi.xclaim.util.ChunkReference;
import codes.wasabi.xclaim.util.DisplayItem;
import codes.wasabi.xclaim.util.InventorySerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class ChunkEditor {

    public static class Events implements Listener {

        private Events() {
            PlatformItemPickupListener listener = Platform.get().getItemPickupListener();
            listener.on(this::onPickup);
            listener.register();
        }

        void tryRegisterConditionalEvents() {
            // Register events that are not guaranteed to exist in Spigot 1.8+
            final String[] miscPlayerEvents = new String[] {
                    "io.papermc.paper.event.player.PlayerItemFrameChangeEvent",
                    "org.bukkit.event.player.PlayerArmorStandManipulateEvent"
            };
            for (String className : miscPlayerEvents) {
                try {
                    this.registerMiscPlayerEvent(Class.forName(className).asSubclass(PlayerEvent.class));
                } catch (ClassNotFoundException | ClassCastException ignored) { }
            }
        }

        private void registerMiscPlayerEvent(@NotNull Class<? extends PlayerEvent> clazz) {
            Bukkit.getPluginManager().registerEvent(
                    clazz,
                    this,
                    EventPriority.NORMAL,
                    (Listener ignored, Event event) -> this.onMiscPlayerEvent((PlayerEvent) event),
                    XClaim.instance
            );
        }

        @EventHandler
        public void onDrop(@NotNull PlayerDropItemEvent event) {
            Player ply = event.getPlayer();
            if (getEditing(ply) != null) {
                event.setCancelled(true);
            }
        }

        public void onPickup(Player ply, Runnable cancel) {
            if (getEditing(ply) != null) {
                cancel.run();
            }
        }

        @EventHandler
        public void onClick(@NotNull InventoryClickEvent event) {
            HumanEntity ent = event.getWhoClicked();
            if (ent instanceof Player) {
                Player ply = (Player) ent;
                if (getEditing(ply) != null) {
                    Inventory inv = event.getClickedInventory();
                    if (Objects.equals(inv, ply.getInventory())) event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onDrag(@NotNull InventoryDragEvent event) {
            HumanEntity ent = event.getWhoClicked();
            if (ent instanceof Player) {
                Player ply = (Player) ent;
                if (getEditing(ply) != null) {
                    Inventory inv = event.getInventory();
                    if (Objects.equals(inv, ply.getInventory())) event.setCancelled(true);
                }
            }
        }

        private boolean checkInventory(@NotNull Inventory inv) {
            if (inv instanceof PlayerInventory) {
                HumanEntity ent = ((PlayerInventory) inv).getHolder();
                if (ent instanceof Player) {
                    return getEditing((Player) ent) != null;
                }
            }
            return false;
        }

        @EventHandler
        public void onMove(@NotNull InventoryMoveItemEvent event) {
            Inventory a = event.getSource();
            Inventory b = event.getDestination();
            if (checkInventory(a)) {
                event.setCancelled(true);
                return;
            }
            if (checkInventory(b)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onInteract(@NotNull PlayerInteractEvent event) {
            Action action = event.getAction();
            if (action == Action.PHYSICAL) return;
            Player ply = event.getPlayer();
            Claim claim = getEditing(ply);
            if (claim != null) {
                event.setCancelled(true);
                PlayerInventory inv = ply.getInventory();
                int slot = inv.getHeldItemSlot();
                switch (slot) {
                    case 1:
                        Chunk chunk = ply.getLocation().getChunk();
                        Claim existing = Claim.getByChunk(chunk);
                        if (existing != null) {
                            if (!existing.getOwner().getUniqueId().equals(ply.getUniqueId())) {
                                if (!(ply.hasPermission("xclaim.override") || ply.isOp())) {
                                    Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-taken"));
                                    break;
                                }
                            }
                        }
                        World w = claim.getWorld();
                        if (w != null) {
                            if (!w.getName().equalsIgnoreCase(chunk.getWorld().getName())) {
                                Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-wrong-world"));
                                break;
                            }
                        }
                        if (ProtectionService.isAvailable()) {
                            ProtectionService service = ProtectionService.getNonNull();
                            Collection<ProtectionRegion> regions = service.getRegionsAt(chunk);
                            boolean all = true;
                            for (ProtectionRegion region : regions) {
                                EnumSet<ProtectionRegion.Permission> set = region.getPermissions(ply);
                                boolean access = Arrays.stream(ProtectionRegion.Permission.values()).allMatch(set::contains);
                                if (!access) {
                                    all = false;
                                    break;
                                }
                            }
                            if (!all) {
                                Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-protection-deny"));
                                break;
                            }
                        }
                        if (violatesDistanceCheck(ply, chunk)) {
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-min-distance-deny"));
                            break;
                        }

                        // ADD THIS BLOCK
                        if (violatesSpawnBoundaryCheck(ply, chunk)) {
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("error.claim-too-far-from-spawn"));
                            break;
                        }

                        final RulesConfig.PlacementRule placementRule = XClaim.mainConfig.rules().placement();
                        if (placementRule != RulesConfig.PlacementRule.NONE) {
                            boolean diagonals = placementRule == RulesConfig.PlacementRule.NEIGHBOR;
                            boolean nextTo = false;
                            int targetX = chunk.getX();
                            int targetZ = chunk.getZ();
                            // gross
                            for (ChunkReference c : claim.getChunks()) {
                                int thisX = c.x;
                                int thisZ = c.z;
                                int leftX = thisX - 1;
                                int rightX = thisX + 1;
                                boolean leftMatch = targetX == leftX;
                                boolean rightMatch = targetX == rightX;
                                if (targetZ == thisZ) {
                                    if (leftMatch) {
                                        nextTo = true;
                                        break;
                                    }
                                    if (rightMatch) {
                                        nextTo = true;
                                        break;
                                    }
                                }
                                int upZ = thisZ + 1;
                                int downZ = thisZ - 1;
                                boolean upMatch = targetZ == upZ;
                                boolean downMatch = targetZ == downZ;
                                if (targetX == thisX) {
                                    if (upMatch) {
                                        nextTo = true;
                                        break;
                                    }
                                    if (downMatch) {
                                        nextTo = true;
                                        break;
                                    }
                                }
                                if (diagonals) {
                                    if (upMatch) {
                                        if (leftMatch || rightMatch) {
                                            nextTo = true;
                                            break;
                                        }
                                    }
                                    if (downMatch) {
                                        if (leftMatch || rightMatch) {
                                            nextTo = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!nextTo) {
                                Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-adjacent"));
                                break;
                            }
                        }
                        XCPlayer xcp = XCPlayer.of(ply);
                        int numChunks = 0;
                        int maxChunks = xcp.getMaxChunks();
                        UUID uuid = ply.getUniqueId();
                        for (Claim c : Claim.getAll()) {
                            if (c.getOwner().getUniqueId().equals(uuid)) {
                                numChunks += c.getChunks().size();
                            }
                        }
                        if (numChunks >= maxChunks) {
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-max"));
                            break;
                        }
                        if (!XClaimEvent.dispatch(new XClaimAddChunkToClaimEvent(ply, claim, chunk))) return;
                        if (claim.addChunk(chunk)) {
                            if (Economy.isAvailable()) {
                                if (numChunks >= xcp.getFreeChunks()) {
                                    Economy eco = Economy.getAssert();
                                    double price = xcp.getClaimPrice();
                                    if (price > 0) {
                                        BigDecimal bd = BigDecimal.valueOf(price);
                                        if (!eco.canAfford(ply, bd)) {
                                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-cant-afford", eco.format(bd)));
                                            claim.removeChunk(chunk);
                                            break;
                                        }
                                        if (!eco.take(ply, bd)) {
                                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-pay-fail", eco.format(bd)));
                                            claim.removeChunk(chunk);
                                            break;
                                        }
                                        Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-pay-success", eco.format(bd)));
                                    }
                                }
                            }
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-add", chunk.getX(), chunk.getZ()));
                        } else {
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-redundant-add"));
                        }
                        break;
                    case 4:
                        Chunk chunk1 = ply.getLocation().getChunk();
                        if (!XClaimEvent.dispatch(new XClaimRemoveChunkFromClaimEvent(ply, claim, chunk1))) return;
                        if (claim.removeChunk(chunk1)) {
                            if (Economy.isAvailable()) {
                                Economy eco = Economy.getAssert();
                                XCPlayer xcp1 = XCPlayer.of(ply);
                                int numChunks1 = 0;
                                UUID uuid1 = ply.getUniqueId();
                                for (Claim c : Claim.getAll()) {
                                    if (c.getOwner().getUniqueId().equals(uuid1)) {
                                        numChunks1 += c.getChunks().size();
                                    }
                                }
                                if (numChunks1 >= xcp1.getFreeChunks()) {
                                    BigDecimal bd = BigDecimal.valueOf(xcp1.getUnclaimReward());
                                    eco.give(ply, bd);
                                    Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-reward", eco.format(bd)));
                                }
                            }
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-remove"));
                        } else {
                            Platform.getAdventure().player(ply).sendMessage(XClaim.lang.getComponent("chunk-editor-redundant-remove"));
                        }
                        break;
                    case 7:
                        stopEditing(ply);
                        break;
                }
            }
        }

        @EventHandler
        public void onInteractEntity(@NotNull PlayerInteractEntityEvent event) {
            Player ply = event.getPlayer();
            if (getEditing(ply) != null) event.setCancelled(true);
        }

        @EventHandler
        public void onLeave(@NotNull PlayerQuitEvent event) {
            Player ply = event.getPlayer();
            if (XClaim.mainConfig.editor().stopOnLeave()) {
                stopEditing(ply);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onDeath(@NotNull PlayerDeathEvent event) {
            Player ply = event.getEntity();
            if (stopEditing(ply)) {
                if (!Platform.get().worldKeepInventory(ply.getWorld())) {
                    List<ItemStack> drops = event.getDrops();
                    drops.clear();
                    drops.addAll(Arrays.asList(ply.getInventory().getContents()));
                }
            }
        }

        @EventHandler
        public void onDamage(@NotNull EntityDamageEvent event) {
            Entity ent = event.getEntity();
            if (!(ent instanceof Player)) return;
            Player ply = (Player) ent;
            if (getEditing(ply) == null) return;

            final ItemStack[] inventory = getRetainedInventory(ply);
            double damage = event.getDamage();
            try {
                damage = codes.wasabi.xclaim.util.AttributeUtil.scaleDamage(
                        damage,
                        (inventory.length > 36) ? inventory[36] : null,
                        (inventory.length > 37) ? inventory[37] : null,
                        (inventory.length > 38) ? inventory[38] : null,
                        (inventory.length > 39) ? inventory[39] : null
                );
            } catch (Throwable ignored) {
                // Attribute APIs may be too modern for the current server environment
                damage *= 0.5d;
            }
            event.setDamage(damage);
        }

        @EventHandler
        public void onMove(@NotNull PlayerMoveEvent event) {
            Player ply = event.getPlayer();
            Claim editing;
            if ((editing = getEditing(ply)) != null) {
                Location from = event.getFrom();
                Location to = event.getTo();
                Chunk fromChunk = from.getChunk();
                Chunk toChunk = to.getChunk();
                if (toChunk.getX() != fromChunk.getX() || toChunk.getZ() != fromChunk.getZ()) {
                    int ownState = 0; // 0=Open, 1=This Claim, 2=Own Other Claim, 3=Taken, 4=Too Far
                    String langUnknown = XClaim.lang.get("unknown");
                    String ownerName = langUnknown;

                    // First, check if the chunk is outside the spawn boundary
                    if (violatesSpawnBoundaryCheck(ply, toChunk)) {
                        ownState = 4;
                    } else {
                        // If it's within the boundary, run the original logic
                        if (editing.contains(to)) {
                            ownState = 1;
                        } else {
                            Claim cl = Claim.getByChunk(toChunk);
                            if (cl != null) {
                                XCPlayer xcp = cl.getOwner();
                                ownerName = xcp.getName();
                                if (ownerName == null) ownerName = langUnknown;
                                ownState = (xcp.getUniqueId().equals(ply.getUniqueId()) ? 2 : 3);
                            }
                        }
                    }

                    Color color = Color.GRAY;
                    String refer = "";
                    switch (ownState) {
                        case 1:
                            color = Color.GREEN;
                            refer = XClaim.lang.get("chunk-editor-info-claimed");
                            break;
                        case 2:
                            color = Color.YELLOW;
                            refer = XClaim.lang.get("chunk-editor-info-owned");
                            break;
                        case 3:
                            color = Color.RED;
                            refer = XClaim.lang.get("chunk-editor-info-taken", ownerName);
                            break;
                        case 4:
                            color = Color.MAROON;
                            refer = XClaim.lang.get("chunk-editor-info-too-far");
                            break;
                        default:
                            color = Color.GRAY;
                            refer = XClaim.lang.get("chunk-editor-info-open");
                            break;
                    }

                    TextColor tc = TextColor.color(color.asRGB());
                    Platform.getAdventure().player(ply).sendMessage(Component.empty()
                            .append(XClaim.lang.getComponent("chunk-editor-info", toChunk.getX(), toChunk.getZ()))
                            .append(Component.newline())
                            .append(Component.text(refer).color(tc))
                    );

                    if (ownState == 4) {
                        ply.playSound(ply.getLocation(), Platform.get().getClickSound(), 0.5f, 0.5f);
                    } else {
                        ply.playSound(ply.getLocation(), Platform.get().getExpSound(), 1f, 1f);
                    }

                    java.awt.Color awtColor = new java.awt.Color(color.asRGB());
                    World w = toChunk.getWorld();
                    double eyeY = to.getY() + ply.getEyeHeight();
                    int targetY = Math.min(Math.max((int) Math.round(eyeY), Platform.get().getWorldMinHeight(w)), w.getMaxHeight() - 1);
                    for (int y = targetY - 2; y < targetY + 3; y++) {
                        Location origin = toChunk.getBlock(0, y, 0).getLocation();
                        for (double x = 0; x <= 16; x += 0.5d) {
                            Location aPos = origin.clone().add(x, 0, 0);
                            Location bPos = origin.clone().add(x, 0, 16);
                            (new ParticleBuilder(ParticleEffect.REDSTONE)).setColor(awtColor).setLocation(aPos).setAmount(1).setOffset(0.02f, 0.02f, 0.02f).display(ply);
                            (new ParticleBuilder(ParticleEffect.REDSTONE)).setColor(awtColor).setLocation(bPos).setAmount(1).setOffset(0.02f, 0.02f, 0.02f).display(ply);
                        }
                        for (double z = 0; z <= 16; z += 0.5d) {
                            Location aPos = origin.clone().add(0, 0, z);
                            Location bPos = origin.clone().add(16, 0, z);
                            (new ParticleBuilder(ParticleEffect.REDSTONE)).setColor(awtColor).setLocation(aPos).setAmount(1).setOffset(0.02f, 0.02f, 0.02f).display(ply);
                            (new ParticleBuilder(ParticleEffect.REDSTONE)).setColor(awtColor).setLocation(bPos).setAmount(1).setOffset(0.02f, 0.02f, 0.02f).display(ply);
                        }
                    }
                }
            }
        }

        public void onMiscPlayerEvent(@NotNull PlayerEvent event) {
            Player ply = event.getPlayer();
            if (getEditing(ply) != null && event instanceof Cancellable) {
                ((Cancellable) event).setCancelled(true);
            }
        }

    }

    private static ItemStack CLAIM_STACK;
    private static ItemStack UNCLAIM_STACK;
    private static ItemStack QUIT_STACK;

    private static PlatformNamespacedKey KEY_FLAG;
    private static PlatformNamespacedKey KEY_NAME;
    private static PlatformNamespacedKey KEY_INVENTORY;
    private static Events EVENTS;
    private static boolean initialized = false;

    public static @NotNull PlatformNamespacedKey getNameKey() {
        return KEY_NAME;
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        CLAIM_STACK = DisplayItem.create(Platform.get().getGreenToken(), XClaim.lang.getComponent("chunk-editor-claim"));
        UNCLAIM_STACK = DisplayItem.create(Platform.get().getRedToken(), XClaim.lang.getComponent("chunk-editor-unclaim"));
        QUIT_STACK = DisplayItem.create(Material.BARRIER, XClaim.lang.getComponent("chunk-editor-quit"));
        KEY_FLAG = Objects.requireNonNull(Platform.get().createNamespacedKey(XClaim.instance, "ce_flag"));
        KEY_NAME = Objects.requireNonNull(Platform.get().createNamespacedKey(XClaim.instance, "ce_name"));
        KEY_INVENTORY = Objects.requireNonNull(Platform.get().createNamespacedKey(XClaim.instance, "ce_inventory"));
        EVENTS = new Events();
        Bukkit.getPluginManager().registerEvents(EVENTS, XClaim.instance);
        EVENTS.tryRegisterConditionalEvents();
    }

    private static final Map<UUID, Claim> editingMap = new HashMap<>();
    public static @Nullable Claim getEditing(@NotNull Player ply) {
        UUID uuid = ply.getUniqueId();
        Claim ret = null;
        if (!editingMap.containsKey(uuid)) {
            PlatformPersistentDataContainer pdc = Platform.get().getPersistentDataContainer(ply);
            if (pdc.has(KEY_FLAG, PlatformPersistentDataType.BYTE)) {
                boolean flag = pdc.getOrDefaultAssert(KEY_FLAG, PlatformPersistentDataType.BYTE, Byte.class, (byte) 0) != ((byte) 0);
                if (flag) {
                    String name = pdc.getAssert(KEY_NAME, PlatformPersistentDataType.STRING, String.class);
                    if (name != null) {
                        ret = Claim.getByName(name);
                        editingMap.put(uuid, ret);
                    }
                }
            }
        } else {
            ret = editingMap.get(uuid);
        }
        return ret;
    }

    public static boolean startEditing(@NotNull Player ply, @NotNull Claim claim) {
        if (getEditing(ply) != null) return false;
        UUID uuid = ply.getUniqueId();
        PlatformPersistentDataContainer pdc = Platform.get().getPersistentDataContainer(ply);
        pdc.set(KEY_NAME, PlatformPersistentDataType.STRING, claim.getName());
        pdc.set(KEY_INVENTORY, PlatformPersistentDataType.BYTE_ARRAY, InventorySerializer.serialize(ply.getInventory()));
        editingMap.put(uuid, claim);
        pdc.set(KEY_FLAG, PlatformPersistentDataType.BYTE, (byte) 1);
        PlayerInventory inv = ply.getInventory();
        inv.clear();
        inv.setItem(1, CLAIM_STACK);
        inv.setItem(4, UNCLAIM_STACK);
        inv.setItem(7, QUIT_STACK);
        return true;
    }

    public static boolean stopEditing(@NotNull Player ply) {
        if (getEditing(ply) == null) return false;
        UUID uuid = ply.getUniqueId();
        PlatformPersistentDataContainer pdc = Platform.get().getPersistentDataContainer(ply);
        pdc.set(KEY_FLAG, PlatformPersistentDataType.BYTE, (byte) 0);
        try {
            ply.getInventory().setContents(getRetainedInventory(ply));
        } catch (IllegalArgumentException e) {
            ply.getInventory().clear();
        }
        editingMap.remove(uuid);
        return true;
    }

    static ItemStack @NotNull [] getRetainedInventory(@NotNull Player ply) throws IllegalArgumentException {
        PlatformPersistentDataContainer pdc = Platform.get().getPersistentDataContainer(ply);
        byte[] inventoryData = pdc.getOrDefaultAssert(KEY_INVENTORY, PlatformPersistentDataType.BYTE_ARRAY, byte[].class, new byte[0]);
        return InventorySerializer.deserialize(inventoryData);
    }

    public static boolean violatesDistanceCheck(Player owner, Chunk chunk) {
        double minDistance = XClaim.mainConfig.rules().minDistance();
        if (minDistance < 1d) return false;
        if (minDistance > 16d) {
            // TODO: Maybe generate a warning here? Checking over 256 chunks just to honor a (probably mistakenly) bad config seems dicey.
            minDistance = 16d;
        }

        final int range = (int) Math.ceil(minDistance);
        final double minDistanceSqr = minDistance * minDistance;
        final ChunkReference start = ChunkReference.ofChunk(chunk);
        double distSqr;

        boolean xZero;
        for (int mX=(-range); mX <= range; mX++) {
            xZero = (mX == 0);
            for (int mZ=(-range); mZ <= range; mZ++) {
                if (xZero && mZ == 0) {
                    continue;
                }
                distSqr = (mX * mX) + (mZ * mZ);
                if (distSqr > minDistanceSqr) continue;

                Claim c = Claim.getByChunk(start.getRelative(mX, mZ));
                if (c == null) continue;
                if (c.getOwner().getUniqueId().equals(owner.getUniqueId())) continue;
                if (c.hasPermission(owner, Permission.MANAGE)) continue;

                return true;
            }
        }

        return false;
    }

    public static boolean violatesSpawnBoundaryCheck(Player ply, Chunk chunk) {
        RulesConfig rules = XClaim.mainConfig.rules();
        int radius = rules.spawnClaimRadius();
        if (radius <= 0) return false;

        if (rules.exemptOpsFromSpawnRestriction() && ply.isOp()) {
            return false;
        }

        if (rules.useSpawnRestrictionWhitelist()) {
            List<String> whitelist = rules.spawnRestrictionWhitelist();
            if (whitelist != null && whitelist.contains(ply.getUniqueId().toString())) {
                return false;
            }
        }

        World world = chunk.getWorld();
        Location spawnLocation = world.getSpawnLocation();
        Chunk spawnChunk = spawnLocation.getChunk();

        int spawnX = spawnChunk.getX();
        int spawnZ = spawnChunk.getZ();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        return Math.abs(chunkX - spawnX) > radius || Math.abs(chunkZ - spawnZ) > radius;
    }
}
