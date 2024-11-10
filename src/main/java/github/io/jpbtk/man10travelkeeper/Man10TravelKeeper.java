package github.io.jpbtk.man10travelkeeper;

import github.io.jpbtk.man10travelkeeper.Commands.man10travelkeeper;
import org.bukkit.plugin.java.JavaPlugin;

public final class Man10TravelKeeper extends JavaPlugin {
    public static Man10TravelKeeper plugin;
    private Listeners listeners;
    public static String prefix = "";

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        plugin.saveDefaultConfig();
        try {
            this.listeners = new Listeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getCommand("man10travelkeeper").setExecutor(new man10travelkeeper());
        } catch (Exception e) {
            e.printStackTrace();
        }
        getServer().getPluginManager().registerEvents(listeners, this);
        plugin.getLogger().info("プラグインが有効化されました。");
        super.onEnable();
        prefix = plugin.getConfig().getString("prefix");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        super.onDisable();
        plugin.getLogger().info("プラグインが無効になりました。");
    }
}
