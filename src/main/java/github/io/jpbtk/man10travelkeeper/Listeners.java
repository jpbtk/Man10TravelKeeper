package github.io.jpbtk.man10travelkeeper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.plugin;
import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.prefix;

public class Listeners implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File file = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId() + ".yml");
        if (!file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            yaml.set("enable", true);
            if (player.isOp()) {
                yaml.set("op-mode", true);
            }
            try {
                yaml.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID world = player.getWorld().getUID();
        File file = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        File playerfile = new File(plugin.getDataFolder(), "playerdata/" + player.getUniqueId() + ".yml");
        YamlConfiguration playeryaml = YamlConfiguration.loadConfiguration(playerfile);
        if (player.isOp() && file.exists() && yaml.getBoolean("op-mode")) {
            player.sendMessage(prefix + plugin.getConfig().getString("message.op-join"));
            return;
        }
        //if ((!(player.hasPermission("man10travelkeeper.travel." + player.getWorld().getUID()))) && yaml.getBoolean("enable"))
        if (!(yaml.getBoolean("enable"))) {
            return;
        }
        // 入場可能な時間帯かどうか
        List<String> settings = yaml.getConfigurationSection("settings").getKeys(false).stream().toList();
        if (settings.isEmpty()) {
            return;
        }
        Date nowdate = new Date(System.currentTimeMillis());
        Boolean isCanEnter = true;
        Integer canEnterTime = 0;
        Date wholeendtime = null;
        for (String setting : settings) {
            if (yaml.getBoolean("settings." + setting + ".enable")) {
                List<String> conditions = (List<String>) yaml.getList("settings." + setting + ".conditions");
                if (!(conditions.isEmpty())) {
                    int j = 0;
                    for (String condition : conditions) {
                        j++;
                        isCanEnter = true;
                        String conditionType = condition.split(":")[0];
                        if (conditionType.equals("date") || conditionType.equals("month") || conditionType.equals("a-day-of-week")) {
                            List<Integer> date = new ArrayList<>();
                            for (String dateStr : condition.split(":")[1].split(",")) {
                                // dateStrが3-5のような範囲指定の場合
                                if (dateStr.contains("-")) {
                                    int start = Integer.parseInt(dateStr.split("-")[0]);
                                    int end = Integer.parseInt(dateStr.split("-")[1]);
                                    for (int i = start; i <= end; i++) {
                                        if (!date.contains(i)) {
                                            date.add(i);
                                        }
                                    }
                                } else {
                                    if (!date.contains(Integer.parseInt(dateStr))) {
                                        date.add(Integer.parseInt(dateStr));
                                    }
                                }
                            }
                            if (!date.contains(nowdate.getDate()) && conditionType.equals("date")) {
                                isCanEnter = false;
                                break;
                            }
                            if (!date.contains(nowdate.getMonth() + 1) && conditionType.equals("month")) {
                                isCanEnter = false;
                                break;
                            }
                            if (!date.contains(nowdate.getDay()) && conditionType.equals("a-day-of-week")) {
                                isCanEnter = false;
                                break;
                            }
                        }
                        if (conditionType.equals("duringtime")) {
                            String time = condition.replace("duringtime:", "");
                            player.sendMessage(time);
                            Date starttime = new Date(System.currentTimeMillis());
                            starttime.setHours(Integer.parseInt(time.split("-")[0].split(":")[0]));
                            starttime.setMinutes(Integer.parseInt(time.split("-")[0].split(":")[1]));
                            starttime.setSeconds(0);
                            Date endtime = new Date(System.currentTimeMillis());
                            endtime.setHours(Integer.parseInt(time.split("-")[1].split(":")[0]));
                            endtime.setMinutes(Integer.parseInt(time.split("-")[1].split(":")[1]));
                            endtime.setSeconds(0);
                            wholeendtime = endtime;
                            if (!(starttime.before(nowdate) && endtime.after(nowdate))) {
                                isCanEnter = false;
                                break;
                            }
                        }
                        if (isCanEnter && j == conditions.size()) {
                            int joinlimit = yaml.getInt("settings." + setting + ".limit");
                            int joincooldown = yaml.getInt("settings." + setting + ".cooldown");
                            if (joinlimit != 0) {
                                if (playeryaml.get(world.toString() + "." + setting + ".join") != null) {
                                    Date lastjoin = new Date(playeryaml.getLong(world.toString() + "." + setting + ".lastjoin"));
                                    // joincooldownは日単位
                                    // lastjoinにjoincooldown日を足して、それがnowdateより前ならjoinlimitをリセット
                                    lastjoin.setDate(lastjoin.getDate() + joincooldown);
                                    if (lastjoin.before(nowdate)) {
                                        playeryaml.set(world.toString() + "." + setting + ".join", 0);
                                    }
                                    try {
                                        playeryaml.save(playerfile);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (playeryaml.get(world.toString() + "." + setting + ".join") != null) {
                                    int join = playeryaml.getInt(world.toString() + "." + setting + ".join");
                                    if (join > joinlimit) {
                                        player.sendMessage(prefix + plugin.getConfig().getString("message.reached-join-limit"));
                                        Location loc = yaml.getLocation("spawn");
                                        player.teleport(loc);
                                        playeryaml.set(world.toString() + "." + setting + ".join", 0);
                                        try {
                                            playeryaml.save(playerfile);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        return;
                                    }
                                    playeryaml.set(world.toString() + "." + setting + ".join", join + 1);
                                    playeryaml.set(world.toString() + "." + setting + ".lastjoin", nowdate.getTime());
                                    try {
                                        playeryaml.save(playerfile);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    playeryaml.set(world.toString() + "." + setting + ".join", 1);
                                    playeryaml.set(world.toString() + "." + setting + ".lastjoin", nowdate.getTime());
                                    try {
                                        playeryaml.save(playerfile);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (!((playeryaml.get(world.toString() + ".time") == null))) {
                                canEnterTime = playeryaml.getInt(world.toString() + ".time");
                            }
                            List<String> actions = yaml.getConfigurationSection("settings." + setting + ".actions").getKeys(false).stream().toList();
                            for (String action : actions) {
                                String actionType = yaml.getString("settings." + setting + ".actions." + action + ".type");
                                if (yaml.get("settings." + setting + ".actions." + action + ".content") != null) {
                                    if (actionType.equals("warp")) {
                                        Location loc = yaml.getLocation("settings." + setting + ".actions." + action + ".content");
                                        player.teleport(loc);
                                    }
                                    if (actionType.equals("message")) {
                                        player.sendMessage(prefix + yaml.getString("settings." + setting + ".actions." + action + ".content"));
                                    }
                                    if (actionType.equals("command")) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), yaml.getString("settings." + setting + ".actions." + action + ".content").replace("%player%", player.getName()));
                                    }
                                }
                            }
                            break;
                        }
                    }
                    if (!isCanEnter) {
                        continue;
                    }
                    canEnterTime = yaml.getInt("settings." + setting + ".time");
                    if (isCanEnter) {
                        break;
                    }
                }
            }
        }
        if (yaml.get("spawn") == null) {
            player.sendMessage(prefix + plugin.getConfig().getString("message.no-spawn"));
            return;
        }
        if (!isCanEnter) {
            player.sendMessage(prefix + plugin.getConfig().getString("message.out-of-time"));
            Location loc = yaml.getLocation("spawn");
            player.teleport(loc);
            return;
        }
        List<OfflinePlayer> players = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        if (yaml.get("players") != null) {
            players = (List<OfflinePlayer>) yaml.getList("players");
        }
        if (yaml.get("dates") != null) {
            dates = (List<Date>) yaml.getList("dates");
        }
        if (!(players.contains(player))) {
            players.add(player);
            dates.add(nowdate);
            yaml.set("players", players);
            yaml.set("dates", dates);
            try {
                yaml.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Date lastdate = dates.get(players.indexOf(player));
            if (nowdate.getTime() - lastdate.getTime() > canEnterTime * 1000) {
                player.sendMessage(prefix + plugin.getConfig().getString("message.reached-time-limit"));
                Location loc = yaml.getLocation("spawn");
                player.teleport(loc);
                players.remove(player);
                dates.remove(lastdate);
                yaml.set("players", players);
                yaml.set("dates", dates);
                try {
                    yaml.save(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            canEnterTime = (int) (canEnterTime - (nowdate.getTime() - lastdate.getTime()) / 1000);
        }
        if (wholeendtime != null) {
            Date endtime = wholeendtime;
            if (endtime.getTime() - nowdate.getTime() < canEnterTime * 1000) {
                canEnterTime = Math.toIntExact((int) canEnterTime - ((endtime.getTime() - nowdate.getTime()) / 1000));
            }
        }
        // 指定秒後にテレポートさせるスレッド処理
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID world2 = player.getWorld().getUID();
            File file2 = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
            YamlConfiguration yaml2 = YamlConfiguration.loadConfiguration(file2);
            player.sendMessage(prefix + plugin.getConfig().getString("message.reached-time-limit"));
            Location loc = yaml.getLocation("spawn");
            player.teleport(loc);
            deletePlayerJoinData(file.getName().replace(".yml", ""), player);
        }, 20L * canEnterTime);
    }
    public void deletePlayerJoinData(String world, Player player) {
        File file = new File(plugin.getDataFolder(), "worlds/" + world + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<OfflinePlayer> players = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        if (yaml.getList("players") != null) {
            players = (List<OfflinePlayer>) yaml.getList("players");
        }
        if (yaml.getList("dates") != null) {
            dates = (List<Date>) yaml.getList("dates");
        }
        if (players.contains(player)) {
            dates.remove(players.indexOf(player));
            players.remove(player);
            yaml.set("players", players);
            yaml.set("dates", dates);
            try {
                yaml.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
