package shake1227.betterroulette.compats;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultProxy {
    private static Economy econ = null;
    public static boolean isVaultLoaded = false;

    public static void setupEconomy() {
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
            Class.forName("org.bukkit.Bukkit");

            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return;
            }
            econ = rsp.getProvider();
            isVaultLoaded = (econ != null);
        } catch (Exception | NoClassDefFoundError e) {
            isVaultLoaded = false;
        }
    }

    public static boolean hasEnough(ServerPlayer player, double amount) {
        if (!isVaultLoaded || amount <= 0) return true;
        return econ.has(player.getBukkitEntity(), amount);
    }

    public static boolean withdraw(ServerPlayer player, double amount) {
        if (!isVaultLoaded || amount <= 0) return true;
        return econ.withdrawPlayer(player.getBukkitEntity(), amount).transactionSuccess();
    }
}