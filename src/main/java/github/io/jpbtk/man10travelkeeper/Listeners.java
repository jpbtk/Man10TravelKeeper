package github.io.jpbtk.man10travelkeeper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.plugin;
import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.prefix;

public class Listeners implements Listener {
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        File file = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (player.isOp()) {
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
        for (String setting : settings) {
            if (yaml.getBoolean("settings." + setting + ".enable")) {
                List<String> conditions = (List<String>) yaml.getList("settings." + setting + ".conditions");
                if (!(conditions.isEmpty())) {
                    for (String condition : conditions) {
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
                            Date starttime = new Date(System.currentTimeMillis());
                            starttime.setHours(Integer.parseInt(time.split("-")[0].split(":")[0]));
                            starttime.setMinutes(Integer.parseInt(time.split("-")[0].split(":")[1]));
                            starttime.setSeconds(0);
                            Date endtime = new Date(System.currentTimeMillis());
                            endtime.setHours(Integer.parseInt(time.split("-")[1].split(":")[0]));
                            endtime.setMinutes(Integer.parseInt(time.split("-")[1].split(":")[1]));
                            endtime.setSeconds(0);
                            if (!(starttime.before(nowdate) && endtime.after(nowdate))) {
                                isCanEnter = false;
                                break;
                            }
                        }
                    }
                    canEnterTime = yaml.getInt("settings." + setting + ".time");
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
            }
            canEnterTime = (int) (canEnterTime - (nowdate.getTime() - lastdate.getTime()) / 1000);
        }
        // 指定秒後にテレポートさせるスレッド処理
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            File file2 = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
            YamlConfiguration yaml2 = YamlConfiguration.loadConfiguration(file2);
            List<OfflinePlayer> players2 = new ArrayList<>();
            if (yaml2.get("players") != null) {
                players2 = (List<OfflinePlayer>) yaml2.getList("players");
            }
            if (players2.contains(player)) {
                players2.remove(player);
                yaml2.set("players", players2);
                try {
                    yaml2.save(file2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            player.sendMessage(prefix + plugin.getConfig().getString("message.reached-time-limit"));
            Location loc = yaml.getLocation("spawn");
            player.teleport(loc);
        }, 20L * canEnterTime);
    }
}
