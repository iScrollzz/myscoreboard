package org.iscrollz.rh.myscoreboard;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Objective;
import org.bukkit.entity.ArmorStand;
import java.util.Comparator;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.stream.Collectors;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.ChatColor;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public final class Myscoreboard extends JavaPlugin implements Listener {

    public static Myscoreboard instance;
    private HashMap<UUID, Integer> kills = new HashMap<>();
    private HashMap<UUID, Double> balances = new HashMap<>();
    public HashMap<UUID, Integer> level = new HashMap<>();
    private HashMap<UUID, UUID> lastDamager = new HashMap<>(); // Хранит последнего игрока, который нанес удар
    private FileConfiguration config;





    @Override
    public void onEnable() {
        instance = this; // Сохраняем экземпляр плагина
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        loadPlayerData();
    }




    @Override
    public void onDisable() {
        savePlayerData();
    }



    public HashMap<UUID, Double> getBalances() {
        return balances; // Метод для получения баланса
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Инициализация данных игрока
        if (!kills.containsKey(player.getUniqueId())) {
            kills.put(player.getUniqueId(), 0);
            balances.put(player.getUniqueId(), 1500.0);
            level.put(player.getUniqueId(), 1);
            saveLevelData();

        }
        updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Entity victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        UUID killerId = lastDamager.get(victimId); // Получаем последнего игрока, который нанес удар

        if (killerId != null && Bukkit.getPlayer(killerId) != null) {
            Player killer = Bukkit.getPlayer(killerId);
            // Увеличение киллов и баланса
            int currentKills = kills.get(killerId);
            kills.put(killerId, currentKills + 1);
            balances.put(killerId, balances.get(killerId) + 250.0);
            killer.sendMessage("§c§lВы получили §e§l250$ §c§lза убийство игрока!");
            updateScoreboard(killer);
        }

        lastDamager.remove(victimId); // Удаляем информацию о последнем ударе
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        UUID killerId = lastDamager.get(victimId); // Получаем последнего игрока, который нанес удар

        // Проверяем, является ли убитая сущность мобом
        if (victim instanceof org.bukkit.entity.Mob) {
            if (killerId != null && Bukkit.getPlayer(killerId) != null) {
                Player killer = Bukkit.getPlayer(killerId);
                balances.put(killerId, balances.get(killerId) + 50.0);
                killer.sendMessage("§c§lВы получили §e§l50$ §c§lза убийство моба!");
                updateScoreboard(killer);
            }

        }

        lastDamager.remove(victimId); // Удаляем информацию о последнем ударе
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Проверяем, является ли атакуемая сущность игроком
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                lastDamager.put(victim.getUniqueId(), attacker.getUniqueId()); // Сохраняем последнего атакующего
            }
        } else if (event.getEntity() instanceof org.bukkit.entity.Mob) {
            // Проверяем, является ли атакующий игроком
            if (event.getDamager() instanceof Player) {
                Player attacker = (Player) event.getDamager();
                lastDamager.put(event.getEntity().getUniqueId(), attacker.getUniqueId()); // Сохраняем последнего атакующего моба
            }
        }
    }



    public double getPlayerBalance(Player player) {
        // Проверяем, есть ли запись для данного игрока
        if (balances.containsKey(player.getUniqueId())) {
            return balances.get(player.getUniqueId());
        } else {
            // Возвращаем 0.0, если игрок не найден в balances
            return 0.0; // или выбросьте исключение, если это более уместно
        }
    }
    public void modifyBalance(Player player, double amount) {
        // Проверяем, есть ли запись для данного игрока
        if (balances.containsKey(player.getUniqueId())) {
            double currentBalance = balances.get(player.getUniqueId());

            // Проверяем, не станет ли баланс отрицательным
            if (currentBalance - amount < 0) {
                // Обработка случая, когда баланс недостаточен
                player.sendMessage("Недостаточно средств для выполнения этой операции.");
                return; // Выход из метода, если недостаточно средств
            }

            // Обновляем баланс
            balances.put(player.getUniqueId(), currentBalance - amount);
            updateScoreboard(player);
        } else {
            // Обработка случая, когда игрок не найден в balances
            player.sendMessage("Ошибка: ваш баланс не найден.");
        }
    }





    private void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("stats", "dummy", ("§5§lSURVIVAL"));
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        // Установка значений
        // objective.getScore("§c§lКиллы:").setScore(kills.get(player.getUniqueId()));
        //  objective.getScore("§a§lБаланс:").setScore(balances.get(player.getUniqueId()).intValue());
        //  objective.getScore("§e§lУровень:").setScore(level.get(player.getUniqueId()).intValue());

        objective.getScore("§6Баланс:" + "    §a§l" +balances.get(player.getUniqueId())).setScore(4);
        objective.getScore("§6Киллы:" + "     §c§l" +kills.get(player.getUniqueId())).setScore(3);
        objective.getScore("§6Уровень:" + "   §6§l" +level.get(player.getUniqueId())).setScore(2);
        objective.getScore("    §b§lskylinemcnet").setScore(1);
        //    int i = level.get(player.getUniqueId()).intValue();




        player.setScoreboard(scoreboard);
    }
    public Integer GetPlayerLevel(Player player) {
        return level.get(player.getUniqueId()).intValue();
    }

    private void loadPlayerData() {
        // Загрузка данных из конфигурации
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            int killCount = config.getInt("players." + key + ".kills");
            double balance = config.getDouble("players." + key + ".balance");
            int levelnow = config.getInt("players." + key + ".level");
            kills.put(uuid, killCount);
            balances.put(uuid, balance);
            level.put(uuid, levelnow );
        }
    }

    private void savePlayerData() {
        // Сохранение данных в конфигурацию
        for (UUID uuid : kills.keySet()) {
            config.set("players." + uuid.toString() + ".kills", kills.get(uuid));
            config.set("players." + uuid.toString() + ".balance", balances.get(uuid));
            config.set("players." + uuid.toString() + ".level", level.get(uuid));
        }
        saveConfig();
    }
    private void saveLevelData() {
        for (UUID uuid : level.keySet()) {
            config.set("players." + uuid.toString() + ".kills", kills.get(uuid));
            config.set("players." + uuid.toString() + ".balance", balances.get(uuid));
            config.set("players." + uuid.toString() + ".level", level.get(uuid));
        }
        saveConfig();
    }

}