package shake1227.betterroulette.compats;

import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;

public class VaultProxy {
    private static Object econ = null;
    public static boolean isVaultLoaded = false;

    private static Method getPlayerMethod;
    private static Method hasMethod;
    private static Method withdrawMethod;
    private static Method transactionSuccessMethod;

    public static void setupEconomy() {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Class<?> serverClass = Class.forName("org.bukkit.Server");
            Class<?> servicesManagerClass = Class.forName("org.bukkit.plugin.ServicesManager");
            Class<?> registeredServiceProviderClass = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
            Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
            Class<?> economyResponseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");

            getPlayerMethod = bukkitClass.getMethod("getPlayer", java.util.UUID.class);

            Method getServerMethod = bukkitClass.getMethod("getServer");
            Object server = getServerMethod.invoke(null);

            Method getServicesManagerMethod = serverClass.getMethod("getServicesManager");
            Object servicesManager = getServicesManagerMethod.invoke(server);

            Method getRegistrationMethod = servicesManagerClass.getMethod("getRegistration", Class.class);
            Object registration = getRegistrationMethod.invoke(servicesManager, economyClass);

            if (registration != null) {
                Method getProviderMethod = registeredServiceProviderClass.getMethod("getProvider");
                econ = getProviderMethod.invoke(registration);

                hasMethod = economyClass.getMethod("has", offlinePlayerClass, double.class);

                withdrawMethod = economyClass.getMethod("withdrawPlayer", offlinePlayerClass, double.class);

                transactionSuccessMethod = economyResponseClass.getMethod("transactionSuccess");

                isVaultLoaded = (econ != null);
            }
        } catch (Exception | NoClassDefFoundError e) {
            isVaultLoaded = false;
            econ = null;
        }
    }

    public static boolean hasEnough(ServerPlayer player, double amount) {
        if (!isVaultLoaded || econ == null || amount <= 0) return true;
        try {
            Object bukkitPlayer = getPlayerMethod.invoke(null, player.getUUID());
            if (bukkitPlayer != null) {
                return (boolean) hasMethod.invoke(econ, bukkitPlayer, amount);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean withdraw(ServerPlayer player, double amount) {
        if (!isVaultLoaded || econ == null || amount <= 0) return true;
        try {
            Object bukkitPlayer = getPlayerMethod.invoke(null, player.getUUID());
            if (bukkitPlayer != null) {
                Object response = withdrawMethod.invoke(econ, bukkitPlayer, amount);
                return (boolean) transactionSuccessMethod.invoke(response);
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}