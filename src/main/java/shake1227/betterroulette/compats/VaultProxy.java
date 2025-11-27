package shake1227.betterroulette.compats;

import net.minecraft.server.level.ServerPlayer;
import shake1227.betterroulette.BetterRoulette;

import java.lang.reflect.Method;

public class VaultProxy {
    private static Object econ = null;
    private static boolean initialized = false;

    private static Method getPlayerMethod;
    private static Method hasMethod;
    private static Method withdrawMethod;
    private static Method transactionSuccessMethod;

    public static boolean checkConnection() {
        if (econ != null) return true;
        setupEconomy();
        return econ != null;
    }

    public static void setupEconomy() {
        if (initialized && econ != null) return;

        try {
            BetterRoulette.LOGGER.info("BetterRoulette: Attempting to hook into Vault...");

            Class<?> bukkitClass;
            try {
                bukkitClass = Class.forName("org.bukkit.Bukkit");
            } catch (ClassNotFoundException e) {
                return;
            }

            Method getServerMethod = bukkitClass.getMethod("getServer");
            Object server = getServerMethod.invoke(null);

            if (server == null) {
                BetterRoulette.LOGGER.warn("BetterRoulette: Bukkit server instance is null. Waiting for server start...");
                return;
            }

            Class<?> economyClass = null;
            try {
                economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            } catch (ClassNotFoundException e) {
                BetterRoulette.LOGGER.info("BetterRoulette: Economy class not found directly. Trying to load via PluginManager...");
                try {
                    Method getPluginManagerMethod = server.getClass().getMethod("getPluginManager");
                    Object pluginManager = getPluginManagerMethod.invoke(server);

                    Method getPluginMethod = pluginManager.getClass().getMethod("getPlugin", String.class);
                    Object vaultPlugin = getPluginMethod.invoke(pluginManager, "Vault");

                    if (vaultPlugin != null) {
                        ClassLoader vaultLoader = vaultPlugin.getClass().getClassLoader();
                        economyClass = Class.forName("net.milkbowl.vault.economy.Economy", true, vaultLoader);
                        BetterRoulette.LOGGER.info("BetterRoulette: Successfully loaded Economy class via Vault ClassLoader.");
                    } else {
                        BetterRoulette.LOGGER.warn("BetterRoulette: Vault plugin instance not found in PluginManager.");
                    }
                } catch (Exception ex) {
                    BetterRoulette.LOGGER.error("BetterRoulette: Failed to load Vault via reflection workaround.", ex);
                }
            }

            if (economyClass == null) {
                BetterRoulette.LOGGER.warn("BetterRoulette: Vault Economy class could not be loaded. Vault integration disabled.");
                return;
            }

            Class<?> servicesManagerClass = Class.forName("org.bukkit.plugin.ServicesManager");
            Class<?> registeredServiceProviderClass = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
            Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer"); // 通常これはBukkitにあるのでそのままロード可能

            if (offlinePlayerClass == null) {
                ClassLoader loader = economyClass.getClassLoader();
                offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer", true, loader);
            }
            Class<?> economyResponseClass;
            try {
                economyResponseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            } catch (ClassNotFoundException e) {
                ClassLoader loader = economyClass.getClassLoader();
                economyResponseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse", true, loader);
            }

            getPlayerMethod = bukkitClass.getMethod("getPlayer", java.util.UUID.class);

            Method getServicesManagerMethod = server.getClass().getMethod("getServicesManager");
            Object servicesManager = getServicesManagerMethod.invoke(server);

            Method getRegistrationMethod = servicesManagerClass.getMethod("getRegistration", Class.class);
            Object registration = getRegistrationMethod.invoke(servicesManager, economyClass);

            if (registration != null) {
                Method getProviderMethod = registeredServiceProviderClass.getMethod("getProvider");
                econ = getProviderMethod.invoke(registration);

                hasMethod = economyClass.getMethod("has", offlinePlayerClass, double.class);
                withdrawMethod = economyClass.getMethod("withdrawPlayer", offlinePlayerClass, double.class);
                transactionSuccessMethod = economyResponseClass.getMethod("transactionSuccess");

                BetterRoulette.LOGGER.info("BetterRoulette: Vault Economy hooked successfully. Provider: " + econ.getClass().getName());
                initialized = true;
            } else {
                BetterRoulette.LOGGER.error("BetterRoulette: Vault is loaded, but no Economy provider (Essentials, CMI, etc.) was found in ServicesManager!");
            }
        } catch (Exception e) {
            BetterRoulette.LOGGER.error("BetterRoulette: Error hooking into Vault", e);
            econ = null;
            initialized = false;
        }
    }

    public static boolean hasEnough(ServerPlayer player, double amount) {
        if (!checkConnection() || amount <= 0) return true;
        try {
            Object bukkitPlayer = getPlayerMethod.invoke(null, player.getUUID());
            if (bukkitPlayer != null) {
                return (boolean) hasMethod.invoke(econ, bukkitPlayer, amount);
            }
        } catch (Exception e) {
            BetterRoulette.LOGGER.error("BetterRoulette: Error checking balance", e);
        }
        return false;
    }

    public static boolean withdraw(ServerPlayer player, double amount) {
        if (!checkConnection() || amount <= 0) return true;
        try {
            Object bukkitPlayer = getPlayerMethod.invoke(null, player.getUUID());
            if (bukkitPlayer != null) {
                boolean has = (boolean) hasMethod.invoke(econ, bukkitPlayer, amount);
                if (!has) return false;

                Object response = withdrawMethod.invoke(econ, bukkitPlayer, amount);
                return (boolean) transactionSuccessMethod.invoke(response);
            }
        } catch (Exception e) {
            BetterRoulette.LOGGER.error("BetterRoulette: Error withdrawing money", e);
        }
        return false;
    }
}