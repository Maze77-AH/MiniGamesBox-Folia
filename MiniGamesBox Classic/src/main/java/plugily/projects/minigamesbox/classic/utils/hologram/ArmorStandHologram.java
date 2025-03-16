/*
 *  MiniGamesBox - Library box with massive content that could be seen as minigames core.
 *  Copyright (C) 2023 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 package plugily.projects.minigamesbox.classic.utils.hologram;

 import org.bukkit.Bukkit;
 import org.bukkit.Location;
 import org.bukkit.World;
 import org.bukkit.entity.ArmorStand;
 import org.bukkit.entity.Entity;
 import org.bukkit.entity.EntityType;
 import org.bukkit.entity.Item;
 import org.bukkit.inventory.ItemStack;
 import org.bukkit.plugin.java.JavaPlugin;
 import org.jetbrains.annotations.NotNull;
 import plugily.projects.minigamesbox.classic.PluginMain;
 import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
 import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.List;
 
 /**
  * ArmorStandHologram creates a hologram consisting of multiple ArmorStands (and optionally an item)
  * and schedules all world interactions on the proper Folia region thread.
  * 
  * Note: All methods that interact with the world (e.g. spawning, querying, or teleporting entities)
  * are now wrapped inside tasks scheduled with getRegionScheduler().
  * 
  * Created at 31.05.2021 by Tigerpanzer_02
  */
 public class ArmorStandHologram {
 
   private Item entityItem;
   private ItemStack item;
   private List<String> lines = new ArrayList<>();
   private Location location;
   private PickupHandler pickupHandler = null;
   private static final PluginMain plugin = JavaPlugin.getPlugin(PluginMain.class);
 
   private final List<ArmorStand> armorStands = new ArrayList<>();
 
   public ArmorStandHologram() {
   }
 
   public ArmorStandHologram(Location location) {
     this.location = location;
     plugin.getHologramManager().getHolograms().add(this);
   }
 
   public ArmorStandHologram(Location location, @NotNull String... lines) {
     this.location = location;
     this.lines = Arrays.asList(lines);
     plugin.getHologramManager().getHolograms().add(this);
     append();
   }
 
   public ArmorStandHologram(Location location, @NotNull List<String> lines) {
     this.location = location;
     this.lines = lines;
     plugin.getHologramManager().getHolograms().add(this);
     append();
   }
 
   public Location getLocation() {
     return location;
   }
 
   public void setLocation(Location location) {
     this.location = location;
   }
 
   public ItemStack getItem() {
     return item;
   }
 
   public Item getEntityItem() {
     return entityItem;
   }
 
   @NotNull
   public List<String> getLines() {
     return lines;
   }
 
   @NotNull
   public List<ArmorStand> getArmorStands() {
     return armorStands;
   }
 
   public ArmorStandHologram overwriteLines(@NotNull String... lines) {
     this.lines = Arrays.asList(lines);
     append();
     return this;
   }
 
   public ArmorStandHologram overwriteLines(@NotNull List<String> lines) {
     this.lines = lines;
     append();
     return this;
   }
 
   public ArmorStandHologram overwriteLine(@NotNull String line) {
     this.lines = Collections.singletonList(line);
     append();
     return this;
   }
 
   public ArmorStandHologram appendLines(@NotNull String... lines) {
     this.lines.addAll(Arrays.asList(lines));
     append();
     return this;
   }
 
   public ArmorStandHologram appendLines(@NotNull List<String> lines) {
     this.lines.addAll(lines);
     append();
     return this;
   }
 
   public ArmorStandHologram appendLine(@NotNull String line) {
     this.lines.add(line);
     append();
     return this;
   }
 
   public ArmorStandHologram appendItem(@NotNull ItemStack item) {
     this.item = item;
     append();
     return this;
   }
 
   /**
    * Moves all ArmorStands to a new location.
    * This method schedules the teleportation on the proper region thread.
    */
   public void moveStands(Location newLocation) {
     World world = newLocation.getWorld();
     if (world == null) return;
     // Use newLocation (which is a Location) for scheduling.
     plugin.getServer().getRegionScheduler().run(plugin, newLocation, task -> {
       double distanceAbove = -0.27;
       double y = newLocation.getY();
       for (ArmorStand armor : armorStands) {
         // Use clone() to avoid modifying the original location.
         armor.teleport(newLocation.clone().add(0, y, 0));
         y += distanceAbove;
       }
     });
   }
 
   /**
    * Deletes all ArmorStands and the associated item from the world.
    * The deletion is scheduled on the proper region thread.
    */
   public void delete() {
     World world = location.getWorld();
     if (world == null) return;
     // Use location for scheduling.
     plugin.getServer().getRegionScheduler().run(plugin, location, task -> {
       for (ArmorStand armor : new ArrayList<>(armorStands)) {
         armor.setCustomNameVisible(false);
         armor.remove();
         plugin.getHologramManager().getArmorStands().remove(armor);
       }
       if (entityItem != null) {
         entityItem.remove();
       }
       plugin.getHologramManager().getHolograms().remove(this);
       armorStands.clear();
     });
   }
 
   public boolean isDeleted() {
     return entityItem == null && armorStands.isEmpty();
   }
 
   /**
    * Schedules the creation of hologram entities on the region thread.
    * All world operations (entity removal, spawning, and item dropping) are executed inside this task.
    */
   private void append() {
     World world = location.getWorld();
     if (world == null) {
       return;
     }
     // Use location for scheduling.
     plugin.getServer().getRegionScheduler().run(plugin, location, task -> {
       // First, delete any existing hologram entities.
       for (ArmorStand armor : new ArrayList<>(armorStands)) {
         armor.setCustomNameVisible(false);
         armor.remove();
         plugin.getHologramManager().getArmorStands().remove(armor);
       }
       armorStands.clear();
 
       double distanceAbove = -0.27;
       double y = location.getY();
 
       for (String line : lines) {
         y += distanceAbove;
         ArmorStand armorStand = getEntityArmorStandInternal(y, world);
         armorStand.setCustomName(line);
         plugin.getDebugger().debug("Creating armorstand with name {0}", line);
         armorStands.add(armorStand);
         plugin.getHologramManager().getArmorStands().add(armorStand);
       }
 
       if (item != null && item.getType() != org.bukkit.Material.AIR) {
         entityItem = world.dropItem(location, item);
         if (VersionUtils.isPaper()) {
           entityItem.setCanMobPickup(false);
         }
         entityItem.setCustomNameVisible(false);
 
         if (ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_10)) {
           entityItem.setGravity(true);
         }
 
         if (ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
           entityItem.setInvulnerable(true);
         }
         VersionUtils.teleport(entityItem, location);
       }
     });
   }
 
   /**
    * Creates an ArmorStand on the provided world at a specified y-coordinate.
    * This method must be called from a task scheduled on the correct region thread.
    *
    * @param y     the y-coordinate for the hologram
    * @param world the world in which to spawn the ArmorStand
    * @return the created ArmorStand
    */
   private ArmorStand getEntityArmorStandInternal(double y, World world) {
     Location loc = location.clone();
     loc.setY(y);
 
     if (ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
       for (Entity entity : world.getNearbyEntities(location, 0.2, 0.2, 0.2)) {
         if (entity instanceof ArmorStand &&
             !armorStands.contains(entity) &&
             !plugin.getHologramManager().getArmorStands().contains(entity)) {
           entity.remove();
           entity.setCustomNameVisible(false);
           plugin.getHologramManager().getArmorStands().remove(entity);
         }
       }
     }
     ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
     stand.setVisible(false);
     stand.setGravity(false);
     stand.setCustomNameVisible(true);
     return stand;
   }
 
   /**
    * Sets a handler which triggers on player pickup item event.
    *
    * @param handler which should be executed on pickup
    */
   public ArmorStandHologram setPickupHandler(PickupHandler handler) {
     plugin.getHologramManager().getHolograms().remove(this);
     this.pickupHandler = handler;
     plugin.getHologramManager().getHolograms().add(this);
     return this;
   }
 
   public PickupHandler getPickupHandler() {
     return pickupHandler;
   }
 
   public boolean hasPickupHandler() {
     return pickupHandler != null;
   }
 }
 