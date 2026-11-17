package de.moinmorni.baumeister.listen;

import de.moinmorni.baumeister.BaumeisterPlugin;
import de.moinmorni.baumeister.Texts;
import de.moinmorni.baumeister.game.Builder;
import de.moinmorni.baumeister.game.Game;
import de.moinmorni.baumeister.game.PlotState;
import de.moinmorni.baumeister.game.Tools;
import de.moinmorni.baumeister.logic.PlotGrid;
import de.moinmorni.baumeister.logic.PlotGrid.Box;
import de.moinmorni.baumeister.logic.Throttle;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

/**
 * Schutz der Parzellen: gebaut wird nur auf der eigenen Parzelle während der Bauzeit, nichts kommt hinaus (Wasser,
 * Kolben, Bäume, fallender Sand), nichts explodiert oder brennt sich weiter. Gegen Lag: höchstens so viele Wesen und
 * Flüssigkeiten je Parzelle, Redstone-Uhren werden gebremst und notfalls abgebaut, Geschosse gibt es keine. Gegen
 * Beleidigungen: Schilder, Namensschilder, Bücher und umbenannte Dinge werden geprüft. Merkt sich außerdem, was sich
 * in jeder Parzelle verändert hat – nur das wird nach der Runde geräumt.
 */
public final class BuildGuard implements Listener {

    private static final Set<EntityType> BANNED_ENTITIES = Set.of(EntityType.WITHER, EntityType.ENDER_DRAGON,
            EntityType.WARDEN, EntityType.ENDERMAN, EntityType.ELDER_GUARDIAN, EntityType.END_CRYSTAL,
            EntityType.TNT, EntityType.TNT_MINECART, EntityType.GHAST, EntityType.SHULKER);
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    /** Bauteile, die eine Redstone-Uhr ausmachen: beim Abbauen nimmt der Schutz sie rund um die Stelle mit. */
    private static final Set<Material> CLOCK_PARTS = Set.of(Material.OBSERVER, Material.REPEATER, Material.COMPARATOR,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH, Material.REDSTONE_WIRE, Material.PISTON,
            Material.STICKY_PISTON);
    /** Höchstens alle 10 Sekunden eine Meldung je Parzelle. */
    private static final long WARN_EVERY = 200;
    private final java.util.Map<Integer, Long> warned = new java.util.HashMap<>();

    private final BaumeisterPlugin plugin;

    public BuildGuard(BaumeisterPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ours(World w) {
        return w == plugin.world();
    }

    private String lang(Player p) {
        return plugin.lang(p);
    }

    /** Parzelle, auf der {@code p} gerade bauen darf, wenn {@code b} darin liegt. */
    private PlotState own(Player p, int x, int y, int z) {
        Game g = plugin.games().playing(p.getUniqueId());
        PlotState s = g == null ? null : g.buildPlot(p.getUniqueId());
        return s != null && s.box().contains(x, y, z) ? s : null;
    }

    private PlotState own(Player p, Block b) {
        return own(p, b.getX(), b.getY(), b.getZ());
    }

    /** Parzelle einer laufenden Bauzeit an dieser Stelle (egal wem). */
    private PlotState building(int x, int y, int z) {
        Game g = plugin.games().at(x, z);
        return g == null || g.phase() != Game.Phase.BUILD ? null : g.plotAt(x, y, z);
    }

    private PlotState building(Block b) {
        return building(b.getX(), b.getY(), b.getZ());
    }

    private void outside(Player p) {
        p.sendActionBar(Texts.text(lang(p), "guard.outside"));
    }

    // ---------------------------------------------------------------- Bauen

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        Block b = e.getBlockPlaced();
        PlotState s = own(p, b);
        if (s == null) {
            e.setCancelled(true);
            outside(p);
            return;
        }
        if (e instanceof BlockMultiPlaceEvent multi) {
            for (BlockState st : multi.getReplacedBlockStates()) {
                if (!s.box().contains(st.getX(), st.getY(), st.getZ())) {
                    e.setCancelled(true);
                    outside(p);
                    return;
                }
            }
        }
        if (Tools.banned(b.getType())) {
            e.setCancelled(true);
            p.sendActionBar(Texts.text(lang(p), "guard.banned"));
            return;
        }
        Builder builder = plugin.games().playing(p.getUniqueId()).builder(p.getUniqueId());
        builder.placed();
        s.touch(b.getX(), b.getY(), b.getZ());
        if (e instanceof BlockMultiPlaceEvent multi) {
            for (BlockState st : multi.getReplacedBlockStates()) {
                s.touch(st.getX(), st.getY(), st.getZ());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        PlotState s = own(p, e.getBlock());
        if (s == null || "wand".equals(de.moinmorni.baumeister.Items.helpTag(p.getInventory().getItemInMainHand()))) {
            e.setCancelled(true);
            if (s == null) {
                outside(p);
            }
            return;
        }
        e.setDropItems(false);
        e.setExpToDrop(0);
        s.touch(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        Block b = e.getBlock();
        PlotState s = own(p, b);
        if (s == null) {
            e.setCancelled(true);
            outside(p);
            return;
        }
        Material m = e.getBucket();
        boolean liquid = m == Material.WATER_BUCKET || m == Material.LAVA_BUCKET || m.name().endsWith("_BUCKET")
                && m != Material.POWDER_SNOW_BUCKET && m != Material.MILK_BUCKET;
        if (liquid) {
            if (!plugin.settings().limits().liquidAllowed(s.liquids())) {
                e.setCancelled(true);
                p.sendMessage(Texts.msg(lang(p), "guard.liquids", Placeholder.unparsed("max",
                        String.valueOf(plugin.settings().limits().liquids()))));
                return;
            }
            s.addLiquid();
        }
        s.touch(b.getX(), b.getY(), b.getZ());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        if (ours(p.getWorld()) && !plugin.builder(p)) {
            PlotState s = own(p, e.getBlock());
            if (s == null) {
                e.setCancelled(true);
            } else {
                s.touch(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ());
            }
        }
    }

    // ---------------------------------------------------------------- Nichts kommt hinaus

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (!ours(e.getBlock().getWorld())) {
            return;
        }
        PlotState from = building(e.getBlock());
        Block to = e.getToBlock();
        if (from == null || !from.box().contains(to.getX(), to.getY(), to.getZ())) {
            e.setCancelled(true);
            return;
        }
        from.touch(to.getX(), to.getY(), to.getZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent e) {
        if (ours(e.getBlock().getWorld()) && !piston(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent e) {
        if (ours(e.getBlock().getWorld()) && !piston(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    private boolean piston(Block piston, List<Block> moved, org.bukkit.block.BlockFace dir) {
        PlotState s = building(piston);
        if (s == null) {
            return false;
        }
        Block head = piston.getRelative(dir);
        if (!s.box().contains(head.getX(), head.getY(), head.getZ())) {
            return false;
        }
        for (Block b : moved) {
            Block to = b.getRelative(dir);
            if (!s.box().contains(b.getX(), b.getY(), b.getZ()) || !s.box().contains(to.getX(), to.getY(), to.getZ())) {
                return false;
            }
            s.touch(to.getX(), to.getY(), to.getZ());
        }
        s.touch(head.getX(), head.getY(), head.getZ());
        return redstone(piston, s) != Throttle.Verdict.REMOVE;
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent e) {
        Block b = e.getBlock();
        if (!ours(b.getWorld())) {
            return;
        }
        PlotState s = building(b);
        if (s == null) {
            Game g = plugin.games().at(b.getX(), b.getZ());
            if (g != null && g.plotAt(b.getX(), b.getY(), b.getZ()) != null) {
                // Nach der Bauzeit steht alles still – auch Redstone.
                e.setNewCurrent(e.getOldCurrent());
            }
            return;
        }
        Throttle.Verdict v = redstone(b, s);
        if (v != Throttle.Verdict.OK) {
            // Aus bleibt aus: so steht die Schaltung still, löst aber beim nächsten Takt wieder aus (und zählt weiter).
            e.setNewCurrent(0);
        }
    }

    /** Redstone-Bremse; beim Dauerläufer wird das Bauteil abgebaut und die Bauleute bekommen Bescheid. */
    private Throttle.Verdict redstone(Block b, PlotState s) {
        Game g = plugin.games().at(b.getX(), b.getZ());
        Throttle.Verdict v = g.redstone().hit(s.index(), Bukkit.getCurrentTick());
        if (v == Throttle.Verdict.REMOVE) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int removed = 0;
                for (Block x : List.of(b, b.getRelative(org.bukkit.block.BlockFace.NORTH),
                        b.getRelative(org.bukkit.block.BlockFace.SOUTH), b.getRelative(org.bukkit.block.BlockFace.EAST),
                        b.getRelative(org.bukkit.block.BlockFace.WEST), b.getRelative(org.bukkit.block.BlockFace.UP),
                        b.getRelative(org.bukkit.block.BlockFace.DOWN))) {
                    if ((x == b || CLOCK_PARTS.contains(x.getType())) && x.getType() != Material.AIR
                            && s.box().contains(x.getX(), x.getY(), x.getZ())) {
                        x.setType(Material.AIR, false);
                        removed++;
                    }
                }
                long now = Bukkit.getCurrentTick();
                Long last = warned.get(g.arena() * PlotGrid.PLOTS + s.index());
                if (removed > 0 && (last == null || now - last > WARN_EVERY)) {
                    warned.put(g.arena() * PlotGrid.PLOTS + s.index(), now);
                    for (java.util.UUID u : s.members()) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null) {
                            p.sendMessage(Texts.msg(lang(p), "guard.redstone", Placeholder.unparsed("pos",
                                    b.getX() + " " + b.getY() + " " + b.getZ())));
                        }
                    }
                }
            });
        }
        return v;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        if (ours(e.getBlock().getWorld()) && !inPlot(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (ours(e.getBlock().getWorld()) && !inPlot(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        if (ours(e.getBlock().getWorld()) && !inPlot(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    private boolean inPlot(Block b) {
        PlotState s = building(b);
        if (s != null) {
            s.touch(b.getX(), b.getY(), b.getZ());
        }
        return s != null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (!ours(e.getBlock().getWorld())) {
            return;
        }
        Player p = e.getPlayer();
        if (p != null && plugin.builder(p)) {
            return;
        }
        if (p == null || own(p, e.getBlock()) == null) {
            e.setCancelled(true);
        } else {
            own(p, e.getBlock()).touch(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTree(StructureGrowEvent e) {
        if (!ours(e.getWorld())) {
            return;
        }
        Block b = e.getLocation().getBlock();
        PlotState s = building(b);
        if (s == null) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(st -> !s.box().contains(st.getX(), st.getY(), st.getZ()));
        e.getBlocks().forEach(st -> s.touch(st.getX(), st.getY(), st.getZ()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        if (!ours(e.getBlock().getWorld())) {
            return;
        }
        PlotState s = building(e.getBlock());
        if (s == null) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(st -> !s.box().contains(st.getX(), st.getY(), st.getZ()));
        e.getBlocks().forEach(st -> s.touch(st.getX(), st.getY(), st.getZ()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent e) {
        if (!ours(e.getBlock().getWorld()) || e.getEntity() instanceof Player) {
            return;
        }
        if (!inPlot(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (ours(e.getLocation().getWorld())) {
            e.blockList().clear();
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.blockList().clear();
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PortalCreateEvent e) {
        if (ours(e.getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        if (ours(e.getFrom().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(EntityPortalEvent e) {
        if (ours(e.getFrom().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPearl(PlayerTeleportEvent e) {
        if (ours(e.getFrom().getWorld()) && Set.of("ENDER_PEARL", "CHORUS_FRUIT", "CONSUMABLE_EFFECT")
                .contains(e.getCause().name())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (ours(e.getWorld()) && e.toWeatherState()) {
            e.setCancelled(true);
        }
    }

    /** Truhen, Hebel, Türen … gehen nur in der eigenen Parzelle während der Bauzeit (sonst bleibt alles, wie es ist). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(org.bukkit.event.player.PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (!ours(p.getWorld()) || plugin.builder(p) || b == null) {
            return;
        }
        if (own(p, b) != null) {
            return;
        }
        if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
            e.setCancelled(true);
        } else if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && b.getType().isInteractable()) {
            e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    // ---------------------------------------------------------------- Wesen

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        Location l = e.getLocation();
        if (!ours(l.getWorld()) || Game.spawningOwn || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        PlotState s = building(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        if (s == null || BANNED_ENTITIES.contains(e.getEntityType())) {
            e.setCancelled(true);
            return;
        }
        if (!plugin.settings().limits().entityAllowed(count(s))) {
            e.setCancelled(true);
            tell(s, "guard.entities");
            return;
        }
        if (e.getEntity() instanceof Mob mob) {
            mob.setAware(false);
            mob.setRemoveWhenFarAway(false);
        }
        e.getEntity().setSilent(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceEntity(EntityPlaceEvent e) {
        Player p = e.getPlayer();
        Location l = e.getEntity().getLocation();
        if (!ours(l.getWorld()) || (p != null && plugin.builder(p))) {
            return;
        }
        PlotState s = p == null ? null : own(p, l.getBlockX(), l.getBlockY(), l.getBlockZ());
        if (s == null || BANNED_ENTITIES.contains(e.getEntityType())) {
            e.setCancelled(true);
            return;
        }
        if (!plugin.settings().limits().entityAllowed(count(s))) {
            e.setCancelled(true);
            tell(s, "guard.entities");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHang(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock().getRelative(e.getBlockFace());
        if (!ours(b.getWorld()) || (p != null && plugin.builder(p))) {
            return;
        }
        PlotState s = p == null ? null : own(p, b);
        if (s == null) {
            e.setCancelled(true);
            return;
        }
        if (!plugin.settings().limits().entityAllowed(count(s))) {
            e.setCancelled(true);
            tell(s, "guard.entities");
        }
    }

    private int count(PlotState s) {
        Box b = s.box();
        int n = 0;
        for (Entity en : plugin.world().getNearbyEntities(new BoundingBox(b.minX(), b.minY() - 1, b.minZ(), b.maxX() + 1,
                b.maxY() + 2, b.maxZ() + 1))) {
            if (!(en instanceof Player) && !(en instanceof Item)) {
                n++;
            }
        }
        return n;
    }

    private void tell(PlotState s, String key) {
        for (java.util.UUID u : s.members()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.sendMessage(Texts.msg(lang(p), key, Placeholder.unparsed("max",
                        String.valueOf(plugin.settings().limits().entities()))));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItem(ItemSpawnEvent e) {
        if (ours(e.getLocation().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent e) {
        if (ours(e.getLocation().getWorld()) && !Game.spawningOwn) {
            e.setCancelled(true);
        }
    }

    /** Darf {@code damager} das Ding an dieser Stelle verändern (eigene Parzelle in der Bauzeit)? */
    private boolean mayTouch(Entity damager, Location at) {
        if (damager instanceof Player p) {
            return plugin.builder(p) || own(p, at.getBlockX(), at.getBlockY(), at.getBlockZ()) != null;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!ours(e.getEntity().getWorld())) {
            return;
        }
        if (e instanceof EntityDamageByEntityEvent by && !(e.getEntity() instanceof Player)
                && mayTouch(by.getDamager(), e.getEntity().getLocation())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent e) {
        if (!ours(e.getEntity().getWorld())) {
            return;
        }
        Location l = e.getEntity().getLocation();
        if (e instanceof HangingBreakByEntityEvent by) {
            if (!mayTouch(by.getRemover(), l)) {
                e.setCancelled(true);
            }
            return;
        }
        if (building(l.getBlockX(), l.getBlockY(), l.getBlockZ()) == null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicle(VehicleDestroyEvent e) {
        if (ours(e.getVehicle().getWorld()) && !mayTouch(e.getAttacker(), e.getVehicle().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicle(VehicleDamageEvent e) {
        if (ours(e.getVehicle().getWorld()) && !mayTouch(e.getAttacker(), e.getVehicle().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStand(PlayerArmorStandManipulateEvent e) {
        if (ours(e.getPlayer().getWorld()) && !mayTouch(e.getPlayer(), e.getRightClicked().getLocation())) {
            e.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- Texte

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSign(SignChangeEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld())) {
            return;
        }
        StringBuilder all = new StringBuilder();
        for (Component c : e.lines()) {
            all.append(c == null ? "" : PLAIN.serialize(c)).append(' ');
        }
        if (!clean(p, all.toString())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBook(PlayerEditBookEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld())) {
            return;
        }
        StringBuilder all = new StringBuilder();
        var meta = e.getNewBookMeta();
        if (meta.hasTitle()) {
            all.append(meta.getTitle()).append(' ');
        }
        for (Component c : meta.pages()) {
            all.append(PLAIN.serialize(c)).append(' ');
        }
        if (!clean(p, all.toString())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnvil(PrepareAnvilEvent e) {
        if (!(e.getView().getPlayer() instanceof Player p) || !ours(p.getWorld())) {
            return;
        }
        String name = e.getView().getRenameText();
        if (name != null && plugin.settings().filter().reason(name) != null) {
            e.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNameTag(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        if (!mayTouch(p, e.getRightClicked().getLocation())) {
            e.setCancelled(true);
            return;
        }
        ItemStack hand = p.getInventory().getItem(e.getHand());
        if (hand.getType() == Material.NAME_TAG && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()
                && !clean(p, PLAIN.serialize(hand.getItemMeta().displayName()))) {
            e.setCancelled(true);
        }
    }

    private boolean clean(Player p, String text) {
        String why = plugin.settings().filter().reason(text);
        if (why == null) {
            return true;
        }
        p.sendMessage(Texts.msg(lang(p), "link".equals(why) ? "guard.link" : "guard.word"));
        return false;
    }

    // ---------------------------------------------------------------- Aufräumen während der Runde

    /** Wesen, die aus einer Parzelle geraten sind (Loren, Boote, Tiere), und alles Herumliegende weg. */
    public void sweep(Game g) {
        Box arena = PlotGrid.arena(g.arena());
        for (Entity e : plugin.world().getNearbyEntities(new BoundingBox(arena.minX(), arena.minY() - 64, arena.minZ(),
                arena.maxX() + 1, arena.maxY() + 64, arena.maxZ() + 1))) {
            if (e instanceof Player || e.getType() == EntityType.FIREWORK_ROCKET) {
                continue;
            }
            Location l = e.getLocation();
            boolean inside = false;
            for (PlotState s : g.plots()) {
                Box b = s.box();
                if (b.containsXz(l.getX(), l.getZ()) && l.getY() >= b.minY() - 2 && l.getY() <= b.maxY() + 4) {
                    inside = true;
                    break;
                }
            }
            if (!inside || e instanceof Item) {
                e.remove();
            }
        }
    }
}
