package me.bram.angelssmp;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AngelsSMP extends JavaPlugin implements Listener, CommandExecutor {

    private final List<String> angelBooks = Arrays.asList("Zoe", "Kore", "Rhea", "Lilana", "Vasia", "Charikleia");
    private final Map<UUID, String> playerBooks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String cmd : List.of("book", "givebook", "angelgui", "myangel")) {
            PluginCommand c = getCommand(cmd);
            if (c != null) c.setExecutor(this);
        }

        new BukkitRunnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    long cd = cooldowns.getOrDefault(p.getUniqueId(), 0L);
                    long left = (System.currentTimeMillis() - cd);
                    String msg = left >= 60000 || cd == 0 ? "§aReady" : "§cCooldown: " + ((60000 - left) / 1000) + "s";
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
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§6Choose Your Angel Book")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && isAngelBook(clicked)) {
                Player p = (Player) e.getWhoClicked();
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName().replace("Angel Book: ", ""));
                this.playerBooks.put(p.getUniqueId(), name);
                for (int i = 0; i < p.getInventory().getSize(); i++) {
                    ItemStack item = p.getInventory().getItem(i);
                    if (isAngelBook(item)) {
                        p.getInventory().setItem(i, null);
                    }
                }

                giveAngelBook(p, name);
                p.sendMessage("§aYou received the Angel Book: §6" + name);
                p.closeInventory();
            }
        }
    }

    private void openCrateGUI(Player p) {
        String selected = angelBooks.get(new Random().nextInt(angelBooks.size()));
        Inventory gui = Bukkit.createInventory(null, 9, "§6Choose Your Angel Book");
        gui.setItem(4, createAngelBook(selected));
        this.playerBooks.put(p.getUniqueId(), selected);
        p.openInventory(gui);
        p.sendMessage("§eOpening crate... You got §6" + selected + "§e!");

        // Sluit de GUI na 10 seconden
        new BukkitRunnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    long cd = cooldowns.getOrDefault(p.getUniqueId(), 0L);
                    long left = 60000 - (System.currentTimeMillis() - cd);
                    if (left <= 0 || cd == 0) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aReady"));
                    } else {
                        long secondsLeft = (left + 999) / 1000; // Rond naar boven af
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cCooldown: " + secondsLeft + "s"));
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    private void giveAngelBook(Player p, String name) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (isAngelBook(item)) {
                p.getInventory().setItem(i, null);
            }
        }

        p.getInventory().addItem(createAngelBook(name));
    }

    private boolean isAngelBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK && item.hasItemMeta()
                && item.getItemMeta().getDisplayName().contains("Angel Book:");
    }

    private ItemStack createAngelBook(String name) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName("§6Angel Book: " + name);
        meta.setLore(List.of("§7Shift + Right Click to activate ability", "§fPower: " + name));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        book.setItemMeta(meta);
        return book;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        ItemStack book = isAngelBook(main) ? main : isAngelBook(off) ? off : null;
        if (book == null || !e.getAction().toString().contains("RIGHT")) return;

        String name = ChatColor.stripColor(book.getItemMeta().getDisplayName().replace("Angel Book: ", ""));
        long cd = cooldowns.getOrDefault(p.getUniqueId(), 0L);
        if (System.currentTimeMillis() - cd < 60000) return;

        switch (name) {
            case "Zoe" -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 5));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 5));
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1, 1);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 30);
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(p) && other.getLocation().distance(p.getLocation()) < 8) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255));
                    }
                }
            }
            case "Kore" -> {
                p.launchProjectile(Fireball.class).setVelocity(p.getEyeLocation().getDirection().multiply(1.2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
                p.getWorld().spawnParticle(Particle.LAVA, p.getLocation(), 20, 1, 1, 1);
            }
            case "Rhea" -> {
                for (Entity entity : p.getNearbyEntities(15, 15, 15)) {
                    if (entity instanceof Player target) {
                        Vector pull = p.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                        target.setVelocity(pull.multiply(0.6));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 4));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    }
                }
                p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation(), 40);
            }
            case "Lilana" -> {
                p.setVelocity(new Vector(0, 1.5, 0));
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (Entity entity : p.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof Player target) {
                            target.damage(4);
                            Vector kb = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                            target.setVelocity(kb.multiply(1.2));
                        }
                    }
                    p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 1);
                }, 20L);
            }
            case "Vasia" -> {
                p.setVelocity(new Vector(0, 1, 0));
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    for (Entity entity : p.getNearbyEntities(6, 6, 6)) {
                        if (entity instanceof Player other) {
                            other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 4));
                        }
                    }
                    p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation(), 30, 2, 2, 2);
                }, 20L);
            }
            case "Charikleia" -> {
                int hits = 0;
                for (Entity entity : p.getNearbyEntities(4, 4, 4)) {
                    if (entity instanceof Player target) {
                        target.damage(4);
                        hits++;
                    }
                }
                double distance = 8 + (hits * 3);
                Vector direction = p.getLocation().getDirection().normalize();
                p.setVelocity(direction.multiply(distance * 0.3));
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 50, 1, 1, 1);
            }
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
            case "book" -> {
                if (playerBooks.containsKey(p.getUniqueId())) {
                    giveAngelBook(p, playerBooks.get(p.getUniqueId()));
                    p.sendMessage("§aYou received your Angel Book again.");
                } else {
                    p.sendMessage("§cNo Angel Book assigned yet.");
                }
                return true;
            }
            case "myangel" -> {
                String b = playerBooks.getOrDefault(p.getUniqueId(), "None");
                p.sendMessage("§aYour current Angel Book: §6" + b);
                return true;
            }
            case "angelgui" -> {
                Inventory gui = Bukkit.createInventory(null, 9, "§6Choose Your Angel Book");
                for (String name : angelBooks) gui.addItem(createAngelBook(name));
                p.openInventory(gui);
                return true;
            }
            case "givebook" -> {
                if (!p.isOp()) return true;
                if (args.length < 2) return false;
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) return true;
                if (!angelBooks.contains(args[1])) return true;
                playerBooks.put(target.getUniqueId(), args[1]);
                giveAngelBook(target, args[1]);
                target.sendMessage("§aYou received Angel Book: §6" + args[1]);
                return true;
            }
        }

        return false;
    }
}