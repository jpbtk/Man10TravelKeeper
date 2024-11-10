package github.io.jpbtk.man10travelkeeper.Commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.plugin;
import static github.io.jpbtk.man10travelkeeper.Man10TravelKeeper.prefix;

public class man10travelkeeper implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§c§lこのコマンドはプレイヤーのみ実行できます。");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper <addworld|delworld|setspawn>");
        }
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(prefix + "§a§l使い方: /man10travelkeeper <addworld|delworld|setspawn>");
                sender.sendMessage(prefix + "§a§l使い方: /man10travelkeeper <list|state|settings>");
                sender.sendMessage(prefix + "§a§l使い方: /man10travelkeeper <reload>");
            }
            if (args[0].equalsIgnoreCase("addworld")) {
                UUID worlduuid = null;
                if (args.length == 1) {
                    worlduuid = (player).getWorld().getUID();
                } else {
                    if (Bukkit.getWorld(args[1]) == null) {
                        sender.sendMessage(prefix + "§c§l指定されたワールドが存在しません。");
                        return true;
                    }
                    worlduuid = Bukkit.getWorld(args[1]).getUID();
                }
                File file = new File(plugin.getDataFolder(), "worlds/" + worlduuid + ".yml");
                if (file.exists()) {
                    sender.sendMessage(prefix + "§c§l指定されたワールドはすでに登録されています。");
                    return true;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                yaml.set("worldname", Bukkit.getWorld(worlduuid).getName());
                yaml.set("spawn", Bukkit.getWorld(worlduuid).getSpawnLocation());
                yaml.set("enable", true);
                try {
                    yaml.save(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sender.sendMessage(prefix + "§a§lワールドを追加しました。");
                // world内にいるplayer
                Player[] players = Bukkit.getWorld(worlduuid).getPlayers().toArray(new Player[0]);
                for (Player p : players) {
                    if (p.hasPermission("man10travelkeeper.travel." + worlduuid) || p.isOp()) {
                        p.sendMessage(prefix + "§a§lあなたはワールドを移動できます。");
                    } else {
                        p.sendMessage(prefix + "§c§lあなたはこのワールドに移動する権限がありません。");
                        Location loc = yaml.getLocation("spawn");
                        p.teleport(loc);
                    }
                }
            }
            if (args[0].equalsIgnoreCase("delworld")) {
                if (args.length == 1) {
                    sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper delworld <worldname>");
                    return true;
                }
                File file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                if (!file.exists()) {
                    sender.sendMessage(prefix + "§c§l指定されたワールドは登録されていません。");
                    return true;
                }
                file.delete();
                sender.sendMessage(prefix + "§a§lワールドを削除しました。");
            }
            if (args[0].equalsIgnoreCase("setspawn") && args.length >= 1 && args.length <= 2) {
                File file = null;
                if (args.length == 1) {
                    file = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
                } else if (args.length == 2) {
                    if (Bukkit.getWorld(args[1]) == null) {
                        sender.sendMessage(prefix + "§c§l指定されたワールドが存在しません。");
                        return true;
                    }
                    file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                }
                if (!file.exists()) {
                    sender.sendMessage(prefix + "§c§lこのワールドは登録されていません。");
                    return true;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Location location = player.getLocation();
                yaml.set("spawn", location);
                try {
                    yaml.save(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sender.sendMessage(prefix + "§a§lスポーン地点を設定しました。");
            }
            if (args[0].equalsIgnoreCase("list")) {
                File[] files = new File(plugin.getDataFolder(), "worlds").listFiles();
                for (File file : files) {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    String enable = yaml.getBoolean("enable") ? "§a§l有効" : "§c§l無効";
                    sender.sendMessage(prefix + "§a§l" + yaml.getString("worldname") + "§r§7: " + file.getName() + enable);
                }
            }
            if (args[0].equalsIgnoreCase("state") && args.length >= 1) {
                File file = null;
                if (args.length == 1) {
                    file = new File(plugin.getDataFolder(), "worlds/" + player.getWorld().getUID() + ".yml");
                } else if (args.length == 2) {
                    if (Bukkit.getWorld(args[1]) == null) {
                        sender.sendMessage(prefix + "§c§l指定されたワールドが存在しません。");
                        return true;
                    }
                    file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                } else if (args.length == 3 && Bukkit.getWorld(args[1]) != null) {
                    Boolean enable = Boolean.parseBoolean(args[2]);
                    file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    if (enable) {
                        yaml.set("enable", true);
                    } else {
                        yaml.set("enable", false);
                    }
                    try {
                        yaml.save(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + "§a§lワールドの状態を" + (enable ? "§a§l有効" : "§c§l無効") + "にしました。");
                    return true;
                } else {
                    sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper state [worldname]");
                    return true;
                }
                if (!file.exists()) {
                    sender.sendMessage(prefix + "§c§lこのワールドは登録されていません。");
                    return true;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                String enable = yaml.getBoolean("enable") ? "§a§l有効" : "§c§l無効";
                sender.sendMessage(prefix + "§a§l" + yaml.getString("worldname") + "§r§7: " + file.getName() + enable);
            }
            if (args[0].equalsIgnoreCase("settings") && args.length >= 1) {
                if (args.length == 1) {
                    sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper settings <create|delete|edit|list>");
                    return true;
                }
                File file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                if (!file.exists()) {
                    sender.sendMessage(prefix + "§c§l指定されたワールドは登録されていません。");
                    return true;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                if (args[2].equalsIgnoreCase("create") && args.length == 4) {
                    if (yaml.getConfigurationSection("settings") == null) {
                        yaml.createSection("settings");
                    }
                    if (yaml.getConfigurationSection("settings").getKeys(false).contains(args[3])) {
                        sender.sendMessage(prefix + "§c§l指定された設定はすでに存在します。");
                        return true;
                    }
                    yaml.set("settings." + args[3] + ".enable", true);
                    try {
                        yaml.save(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + "§a§l設定を追加しました。");
                }
                if (args[2].equalsIgnoreCase("delete") && args.length == 4) {
                    if (yaml.getConfigurationSection("settings") == null) {
                        sender.sendMessage(prefix + "§c§l設定が存在しません。");
                        return true;
                    }
                    if (!yaml.getConfigurationSection("settings").getKeys(false).contains(args[3])) {
                        sender.sendMessage(prefix + "§c§l指定された設定は存在しません。");
                        return true;
                    }
                    yaml.set("settings." + args[3], null);
                    try {
                        yaml.save(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + "§a§l設定を削除しました。");
                }
                if (args[2].equalsIgnoreCase("edit") && args.length >= 4) {
                    if (args.length == 4) {
                        sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper settings edit <settingname>");
                        return true;
                    }
                    if (!yaml.getConfigurationSection("settings").getKeys(false).contains(args[3])) {
                        sender.sendMessage(prefix + "§c§l指定された設定は存在しません。");
                        return true;
                    }
                    if (args.length == 5) {
                        sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper settings edit <settingname> <enable|time|conditions>");
                        return true;
                    }
                    if (args[4].equalsIgnoreCase("enable") && args.length == 6) {
                        Boolean enable = Boolean.parseBoolean(args[5]);
                        yaml.set("settings." + args[3] + ".enable", enable);
                        try {
                            yaml.save(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sender.sendMessage(prefix + "§a§l設定を変更しました。");
                    }
                    if (args[4].equalsIgnoreCase("time") && args.length == 6) {
                        yaml.set("settings." + args[3] + ".time", Integer.parseInt(args[5]));
                        try {
                            yaml.save(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sender.sendMessage(prefix + "§a§l設定を変更しました。");
                    }
                    if (args[4].equalsIgnoreCase("conditions") && args.length >= 6) {
                        if (args[5].equalsIgnoreCase("add") && args.length >= 7) {
                            if (args.length == 7) {
                                sender.sendMessage(prefix + "§c§l使い方: /man10travelkeeper settings edit <settingname> conditions add <date|a-day-of-week|duringtime|month>");
                                return true;
                            }
                            List<String> conditions = new ArrayList<>();
                            if (yaml.getList("settings." + args[3] + ".conditions") != null) {
                                conditions = (List<String>) yaml.getList("settings." + args[3] + ".conditions");
                            }
                            String str = args[6] + ":" + args[7];
                            conditions.add(str);
                            yaml.set("settings." + args[3] + ".conditions", conditions);
                            try {
                                yaml.save(file);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            sender.sendMessage(prefix + "§a§l条件を追加しました。");
                        }
                        if (args[5].equalsIgnoreCase("delete") && args.length == 7) {
                            List<String> conditions = (List<String>) yaml.getList("settings." + args[3] + ".conditions");
                            if (conditions == null) {
                                sender.sendMessage(prefix + "§c§l条件が存在しません。");
                                return true;
                            }
                            if (conditions.contains(args[6])) {
                                conditions.remove(args[6]);
                                yaml.set("settings." + args[3] + ".conditions", conditions);
                                try {
                                    yaml.save(file);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                sender.sendMessage(prefix + "§a§l条件を削除しました。");
                            } else {
                                sender.sendMessage(prefix + "§c§l指定された条件は存在しません。");
                            }
                        }
                        if (args[5].equalsIgnoreCase("list")) {
                            List<String> conditions = (List<String>) yaml.getList("settings." + args[3] + ".conditions");
                            if (conditions == null) {
                                sender.sendMessage(prefix + "§c§l条件が存在しません。");
                                return true;
                            }
                            sender.sendMessage(prefix + "§a§l条件一覧:");
                            for (String condition : conditions) {
                                sender.sendMessage("§8- §7" + condition);
                            }
                        }
                    }
                }
                if (args[2].equalsIgnoreCase("list")) {
                    List<String> settings = yaml.getConfigurationSection("settings").getKeys(false).stream().toList();
                    if (settings.size() == 0) {
                        sender.sendMessage(prefix + "§c§l設定が存在しません。");
                        return true;
                    }
                    sender.sendMessage(prefix + "§a§l設定一覧:");
                    for (String setting : settings) {
                        Boolean enable = yaml.getBoolean("settings." + setting + ".enable");
                        sender.sendMessage("§8- §7" + setting + "§r§7: " + (enable ? "§a有効" : "§c無効"));
                    }
                }
            }
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                sender.sendMessage(prefix + "§a§lコンフィグをリロードしました。");
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }
        Player player = (Player) sender;
        List<String> tab = new ArrayList<>();
        if (args.length == 1) {
            tab.add("help");
            tab.add("addworld");
            tab.add("delworld");
            tab.add("setspawn");
            tab.add("list");
            tab.add("state");
            tab.add("settings");
            tab.add("reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("addworld")) {
                World[] worlds = Bukkit.getWorlds().toArray(new World[0]);
                for (World world : worlds) {
                    tab.add(world.getName());
                }
            }
            if (args[0].equalsIgnoreCase("delworld") || args[0].equalsIgnoreCase("setsapwn") || args[0].equalsIgnoreCase("state") || args[0].equalsIgnoreCase("settings")) {
                File[] files = new File(plugin.getDataFolder(), "worlds").listFiles();
                for (File file : files) {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    tab.add(yaml.getString("worldname"));
                }
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("state")) {
                tab.add("true");
                tab.add("false");
            }
            if (args[0].equalsIgnoreCase("settings")) {
                tab.add("create");
                tab.add("delete");
                tab.add("edit");
                tab.add("list");
            }
        }
        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("settings")) {
                File file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
                if (!file.exists()) {
                    return null;
                }
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                if (args[2].equalsIgnoreCase("create")) {
                    tab.add("<任意の時間設定名>");
                }
                if (args[2].equalsIgnoreCase("delete")) {
                    List<String> settings = yaml.getConfigurationSection("settings").getKeys(false).stream().toList();
                    for (String setting : settings) {
                        tab.add(setting);
                    }
                }
                if (args[2].equalsIgnoreCase("edit")) {
                    List<String> settings = yaml.getConfigurationSection("settings").getKeys(false).stream().toList();
                    for (String setting : settings) {
                        tab.add(setting);
                    }
                }
                if (args[2].equalsIgnoreCase("list")) {
                    List<String> settings = yaml.getConfigurationSection("settings").getKeys(false).stream().toList();
                    for (String setting : settings) {
                        tab.add(setting);
                    }
                }
            }
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("settings") && args[2].equalsIgnoreCase("edit")) {
            File file = new File(plugin.getDataFolder(), "worlds/" + Bukkit.getWorld(args[1]).getUID() + ".yml");
            if (!file.exists()) {
                return null;
            }
            if (args.length == 5) {
                tab.add("conditions");
                tab.add("time");
                //tab.add("limit");
                tab.add("enable");
            }
            if (args.length == 6) {
                if (args[4].equalsIgnoreCase("conditions")) {
                    tab.add("add");
                    tab.add("delete");
                    tab.add("list");
                }
                if (args[4].equalsIgnoreCase("time")) {
                    tab.add("<滞在可能時間(秒)>");
                }
                if (args[4].equalsIgnoreCase("limit")) {
                    //tab.add("<入場可能回数>");
                }
                if (args[4].equalsIgnoreCase("enable")) {
                    tab.add("true");
                    tab.add("false");
                }
            }
            if (args.length == 7 && args[4].equalsIgnoreCase("conditions") && args[5].equalsIgnoreCase("add")) {
                tab.add("date");
                tab.add("a-day-of-week");
                tab.add("duringtime");
                tab.add("month");
            }
            if (args.length == 7 && args[4].equalsIgnoreCase("conditions") && args[5].equalsIgnoreCase("delete")) {
                List<String> conditions = (List<String>) YamlConfiguration.loadConfiguration(file).getList("settings." + args[3] + ".conditions");
                for (String condition : conditions) {
                    tab.add(condition);
                }
            }
            if (args.length == 8 && args[4].equalsIgnoreCase("conditions") && args[5].equalsIgnoreCase("add")) {
                if (args[6].equalsIgnoreCase("date")) {
                    tab.add("<日付(,で区切って入力、1-3,5,10で1,2,3,5,10日が指定できる)>");
                }
                if (args[6].equalsIgnoreCase("a-day-of-week")) {
                    tab.add("<曜日(0-4,6で日曜日から木曜日,土曜日が指定できる)>");
                }
                if (args[6].equalsIgnoreCase("duringtime")) {
                    tab.add("<時間(18:00-20:00で18時から20時まで)>");
                }
                if (args[6].equalsIgnoreCase("month")) {
                    tab.add("<月(6-8,10で6,7,8,10月が指定できる)>");
                }
            }
        }
        if (player.hasPermission("man10travelkeeper.use")) {
            return tab;
        }
        return null;
    }
}