package me.bram.angelssmp;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class AngelsSMP extends JavaPlugin implements Listener, CommandExecutor {

    private final List<String> angelBooks = Arrays.asList("Zoe", "Kore", "Rhea", "Lilana", "Vasia", "Charikleia");
    private final Map<UUID, String> playerBooks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final NamespacedKey angelKey;

    public AngelsSMP() {
        this.angelKey = new NamespacedKey(this, "angelbook");
    }

    private void removeAngelBooksFromInventory(PlayerInventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isAngelBook(item)) {
                inventory.setItem(i, null);
            }
        }
    }
    @Override
    public void onLoad() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        getLogger().info("AngelsSMP is loading...");
    }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String cmd : List.of("book", "givebook", "angelgui", "myangel")) {
            PluginCommand c = getCommand(cmd);
            if (c != null) c.setExecutor(this);
        }
        getLogger().info("AngelsSMP has been enabled!");
        new BukkitRunnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    long cd = cooldowns.getOrDefault(p.getUniqueId(), 0L);
                    long left = System.currentTimeMillis() - cd;
                    String msg = left >= 60000 || cd == 0 ? "§aReady" : "§cCooldown: " + ((60000 - left + 999) / 1000) + "s";
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!playerBooks.containsKey(p.getUniqueId())) {
            openCrateGUI(p);
        } else {
            giveAngelBook(p, playerBooks.get(p.getUniqueId()));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        e.getDrops().removeIf(this::isAngelBook);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (playerBooks.containsKey(p.getUniqueId())) {
                giveAngelBook(p, playerBooks.get(p.getUniqueId()));
            }
        }, 20L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isAngelBook(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot drop Angel Tomes!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§6Choose Your Angel Tome")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && isAngelBook(clicked)) {
                Player p = (Player) e.getWhoClicked();
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName().replace("§6§lAngel Tome: ", ""));
                this.playerBooks.put(p.getUniqueId(), name);
                removeAngelBooksFromInventory(p.getInventory());
                giveAngelBook(p, name);
                p.sendMessage("§aYou received the Angel Tome: §6" + name);
                p.closeInventory();
            }
        }
    }

    private void openCrateGUI(Player p) {
        String selected = angelBooks.get(new Random().nextInt(angelBooks.size()));
        Inventory gui = Bukkit.createInventory(null, 9, "§6Choose Your Angel Tome");
        gui.setItem(4, createAngelBook(selected));
        this.playerBooks.put(p.getUniqueId(), selected);
        p.openInventory(gui);
        p.sendMessage("§eOpening crate... You got §6" + selected + "§e!");
    }

    private void giveAngelBook(Player p, String name) {
        removeAngelBooksFromInventory(p.getInventory());
        p.getInventory().addItem(createAngelBook(name));
    }

    private boolean isAngelBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(angelKey, PersistentDataType.STRING);
    }

    private ItemStack createAngelBook(String name) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        meta.setDisplayName("§6§lAngel Tome: " + name);
        meta.setLore(Arrays.asList(
                "§7A sacred text containing",
                "§7the power of " + name,
                "",
                "§eShift + Right Click to activate",
                "§eCooldown: 60 seconds"
        ));

        int customModelData = switch (name) {
            case "Zoe" -> 1001;
            case "Kore" -> 1002;
            case "Rhea" -> 1003;
            case "Lilana" -> 1004;
            case "Vasia" -> 1005;
            case "Charikleia" -> 1006;
            default -> 0;
        };
        meta.setCustomModelData(customModelData);

        meta.getPersistentDataContainer().set(angelKey, PersistentDataType.STRING, name);

        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        book.setItemMeta(meta);
        return book;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking() || !e.getAction().toString().contains("RIGHT")) return;

        ItemStack book = isAngelBook(p.getInventory().getItemInMainHand()) ? p.getInventory().getItemInMainHand() :
                isAngelBook(p.getInventory().getItemInOffHand()) ? p.getInventory().getItemInOffHand() : null;
        if (book == null) return;

        String name = book.getItemMeta().getPersistentDataContainer().get(angelKey, PersistentDataType.STRING);
        long cd = cooldowns.getOrDefault(p.getUniqueId(), 0L);
        if (System.currentTimeMillis() - cd < 60000) {
            p.sendMessage(ChatColor.RED + "Ability is on cooldown!");
            return;
        }

        Location loc = p.getLocation();
        World world = p.getWorld();

        // Activation effects
        world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 1);
        world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.8f, 1.2f);
        world.spawnParticle(Particle.FIREWORK, loc, 50, 0.5, 0.5, 0.5, 0.2);
        world.spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.5, 0.5, 0.1);

        switch (name) {
            case "Zoe":
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 5));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 5));

                new BukkitRunnable() {
                    double radius = 0;
                    public void run() {
                        radius += 0.5;
                        if (radius > 8) {
                            this.cancel();
                            return;
                        }
                        for (int i = 0; i < 36; i++) {
                            double angle = i * 10;
                            double x = radius * Math.cos(Math.toRadians(angle));
                            double z = radius * Math.sin(Math.toRadians(angle));
                            Location particleLoc = loc.clone().add(x, 0.2, z);
                            world.spawnParticle(Particle.HEART, particleLoc, 1);
                            world.spawnParticle(Particle.DUST, particleLoc, 1,
                                    new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.5f));
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                world.playSound(loc, Sound.BLOCK_BELL_USE, 1, 1);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 100, 2, 2, 2);
                world.spawnParticle(Particle.INSTANT_EFFECT, loc, 50, 1, 1, 1);

                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(p) && other.getLocation().distance(loc) < 8) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255));
                        world.spawnParticle(Particle.LARGE_SMOKE, other.getLocation(), 20);
                    }
                }
                break;

            case "Kore":
                Fireball fireball = p.launchProjectile(Fireball.class);
                fireball.setVelocity(p.getEyeLocation().getDirection().multiply(1.5));
                fireball.setIsIncendiary(true);

                new BukkitRunnable() {
                    public void run() {
                        if (fireball.isDead()) {
                            this.cancel();
                            return;
                        }
                        Location fbLoc = fireball.getLocation();
                        world.spawnParticle(Particle.FLAME, fbLoc, 20, 0.1, 0.1, 0.1, 0.05);
                        world.spawnParticle(Particle.LAVA, fbLoc, 5);
                        world.spawnParticle(Particle.LARGE_SMOKE, fbLoc, 3);
                        world.spawnParticle(Particle.FIREWORK, fbLoc, 5, 0.1, 0.1, 0.1, 0.05);
                        if (fbLoc.getBlock().getType() == Material.AIR) {
                            fbLoc.getBlock().setType(Material.FIRE);
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
                world.spawnParticle(Particle.LAVA, loc, 30, 1, 1, 1);
                world.spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.5, 0.5, 0.1);

                for (int i = 0; i < 3; i++) {
                    final int tier = i;
                    new BukkitRunnable() {
                        public void run() {
                            double radius = 2.0 + (tier * 0.8);
                            for (int j = 0; j < 36; j++) {
                                double angle = j * 10 + (System.currentTimeMillis()/50 % 360);
                                double x = radius * Math.cos(Math.toRadians(angle));
                                double z = radius * Math.sin(Math.toRadians(angle));
                                Location particleLoc = loc.clone().add(x, 0.2, z);
                                world.spawnParticle(Particle.FLAME, particleLoc, 1);
                            }
                        }
                    }.runTaskTimer(this, i * 5, 1);
                }
                break;

            case "Rhea":
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.8f);

                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        ticks++;
                        if (ticks > 20) {
                            this.cancel();
                            return;
                        }
                        for (int i = 0; i < 3; i++) {
                            double radius = ticks * 0.5;
                            double angle = ticks * 15 + (i * 120);
                            double x = radius * Math.cos(Math.toRadians(angle));
                            double z = radius * Math.sin(Math.toRadians(angle));
                            Location particleLoc = loc.clone().add(x, 0.5, z);
                            world.spawnParticle(Particle.PORTAL, particleLoc, 2);
                            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 1);
                        }
                        for (Entity entity : p.getNearbyEntities(15, 15, 15)) {
                            if (entity instanceof Player target) {
                                Vector pull = loc.toVector().subtract(target.getLocation().toVector()).normalize();
                                target.setVelocity(pull.multiply(0.6));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                                Location targetLoc = target.getLocation();
                                Vector direction = loc.toVector().subtract(targetLoc.toVector()).normalize();
                                for (int j = 0; j < 5; j++) {
                                    Location particlePoint = targetLoc.clone().add(
                                            direction.clone().multiply(j * 0.5));
                                    world.spawnParticle(Particle.WITCH, particlePoint, 1);
                                }
                            }
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 100, 2, 2, 2);
                    world.spawnParticle(Particle.DRAGON_BREATH, loc, 50, 1, 1, 1);
                    world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1);
                }, 20L);
                break;

            case "Lilana":
                p.setVelocity(new Vector(0, 1.5, 0));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);

                new BukkitRunnable() {
                    public void run() {
                        if (p.getVelocity().getY() <= 0) {
                            this.cancel();
                            return;
                        }
                        Location current = p.getLocation();
                        world.spawnParticle(Particle.CLOUD, current, 10, 0.2, 0.2, 0.2, 0.1);
                        world.spawnParticle(Particle.FIREWORK, current, 5, 0.2, 0.2, 0.2, 0.1);
                    }
                }.runTaskTimer(this, 0, 1);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    world.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1, 1);
                    for (int i = 0; i < 36; i++) {
                        final int angle = i * 10;
                        new BukkitRunnable() {
                            public void run() {
                                double x = Math.cos(Math.toRadians(angle));
                                double z = Math.sin(Math.toRadians(angle));
                                Vector direction = new Vector(x, 0.2, z);
                                for (int j = 0; j < 5; j++) {
                                    Location point = loc.clone().add(direction.clone().multiply(j));
                                    world.spawnParticle(Particle.CRIT, point, 2);
                                    if (j == 4) {
                                        world.spawnParticle(Particle.EXPLOSION, point, 3);
                                    }
                                }
                            }
                        }.runTaskLater(this, i / 6);
                    }
                    for (Entity entity : p.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof Player target) {
                            target.damage(4);
                            Vector kb = target.getLocation().toVector().subtract(loc.toVector()).normalize();
                            target.setVelocity(kb.multiply(1.2));
                            world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation(), 10);
                        }
                    }
                }, 20L);
                break;

            case "Vasia":
                p.setVelocity(new Vector(0, 1, 0));
                world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1, 1);

                new BukkitRunnable() {
                    public void run() {
                        if (p.getVelocity().getY() <= 0) {
                            this.cancel();
                            return;
                        }
                        Location current = p.getLocation();
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, 15, 0.2, 0.2, 0.2, 0.1);
                        world.spawnParticle(Particle.SOUL, current, 5, 0.3, 0.3, 0.3);
                    }
                }.runTaskTimer(this, 0, 1);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1, 0.8f);
                    for (int r = 1; r <= 6; r++) {
                        final int radius = r;
                        new BukkitRunnable() {
                            public void run() {
                                for (int i = 0; i < 36; i++) {
                                    double angle = i * 10;
                                    double x = radius * Math.cos(Math.toRadians(angle));
                                    double z = radius * Math.sin(Math.toRadians(angle));
                                    Location particleLoc = loc.clone().add(x, 0.1, z);
                                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1);
                                }
                            }
                        }.runTaskLater(this, r * 2);
                    }
                    for (Entity entity : p.getNearbyEntities(6, 6, 6)) {
                        if (entity instanceof Player other) {
                            other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 4));
                            world.spawnParticle(Particle.SOUL, other.getLocation().add(0, 1, 0), 15);
                        }
                    }
                }, 20L);
                break;

            case "Charikleia":
                int hits = 0;
                for (Entity entity : p.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof Player target) {
                        target.damage(4);
                        hits++;
                        world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 20);
                        world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.8f);

                        // Apply effects to nearby players
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    }
                }

                double distance = 10 + (hits * 4);
                Vector direction = loc.getDirection().normalize();
                p.setVelocity(direction.multiply(distance * 0.5));

                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        ticks++;
                        if (ticks > 20) {
                            this.cancel();
                            return;
                        }
                        Location current = p.getLocation();
                        world.spawnParticle(Particle.ELECTRIC_SPARK, current, 15, 0.3, 0.3, 0.3, 0.1);
                        world.spawnParticle(Particle.CLOUD, current, 10, 0.3, 0.3, 0.3);
                        world.spawnParticle(Particle.FIREWORK, current, 8, 0.2, 0.2, 0.2, 0.05);
                        if (ticks % 3 == 0) {
                            world.spawnParticle(Particle.REVERSE_PORTAL, current, 3);
                        }
                    }
                }.runTaskTimer(this, 0, 1);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    world.spawnParticle(Particle.EXPLOSION, p.getLocation(), 1);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 100, 2, 2, 2);
                    world.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 0.8f);
                    new BukkitRunnable() {
                        int sparks = 0;
                        public void run() {
                            if (sparks++ > 5) this.cancel();
                            world.spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 20, 1, 1, 1);
                        }
                    }.runTaskTimer(this, 0, 5);
                }, 5L);
                break;
        }

        cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "book":
                if (playerBooks.containsKey(p.getUniqueId())) {
                    giveAngelBook(p, playerBooks.get(p.getUniqueId()));
                    p.sendMessage("§aYou received your Angel Tome again.");
                } else {
                    p.sendMessage("§cNo Angel Tome assigned yet.");
                }
                return true;
            case "myangel":
                p.sendMessage("§aYour current Angel Tome: §6" + playerBooks.getOrDefault(p.getUniqueId(), "None"));
                return true;
            case "angelgui":
                if (!p.hasPermission("angelssmp.gui")) {
                    p.sendMessage(ChatColor.RED + "You don't have permission for this!");
                    return true;
                }
                Inventory gui = Bukkit.createInventory(null, 9, "§6Choose Your Angel Tome");
                angelBooks.forEach(name -> gui.addItem(createAngelBook(name)));
                p.openInventory(gui);
                return true;
            case "givebook":
                if (!p.hasPermission("angelssmp.give")) {
                    p.sendMessage(ChatColor.RED + "You don't have permission for this!");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Usage: /givebook <player> <book>");
                    return false;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !angelBooks.contains(args[1])) {
                    p.sendMessage(ChatColor.RED + "Invalid player or book name!");
                    return true;
                }
                playerBooks.put(target.getUniqueId(), args[1]);
                giveAngelBook(target, args[1]);
                p.sendMessage("§aGiven Angel Tome §6" + args[1] + " §ato §6" + target.getName());
                return true;
            default:
                return false;
        }
    }
}
