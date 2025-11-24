package shake1227.betterroulette.compats;

import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;

public class VaultProxy {
    // 依存関係がない環境でも読み込めるよう、型はObjectにする
    private static Object econ = null;
    public static boolean isVaultLoaded = false;

    // リフレクション用メソッドキャッシュ
    private static Method getPlayerMethod;
    private static Method hasMethod;
    private static Method withdrawMethod;
    private static Method transactionSuccessMethod;

    public static void setupEconomy() {
        try {
            // クラスの存在確認 (Bukkit環境でなければここで例外が発生してcatchブロックへ飛ぶ)
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Class<?> serverClass = Class.forName("org.bukkit.Server");
            Class<?> servicesManagerClass = Class.forName("org.bukkit.plugin.ServicesManager");
            Class<?> registeredServiceProviderClass = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
            Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
            Class<?> economyResponseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");

            // メソッドの取得: Bukkit.getPlayer(UUID)
            getPlayerMethod = bukkitClass.getMethod("getPlayer", java.util.UUID.class);

            // Bukkit.getServer()
            Method getServerMethod = bukkitClass.getMethod("getServer");
            Object server = getServerMethod.invoke(null);

            // server.getServicesManager()
            Method getServicesManagerMethod = serverClass.getMethod("getServicesManager");
            Object servicesManager = getServicesManagerMethod.invoke(server);

            // servicesManager.getRegistration(Economy.class)
            Method getRegistrationMethod = servicesManagerClass.getMethod("getRegistration", Class.class);
            Object registration = getRegistrationMethod.invoke(servicesManager, economyClass);

            if (registration != null) {
                // registration.getProvider()
                Method getProviderMethod = registeredServiceProviderClass.getMethod("getProvider");
                econ = getProviderMethod.invoke(registration);

                // Economy.has(OfflinePlayer, double)
                hasMethod = economyClass.getMethod("has", offlinePlayerClass, double.class);

                // Economy.withdrawPlayer(OfflinePlayer, double)
                withdrawMethod = economyClass.getMethod("withdrawPlayer", offlinePlayerClass, double.class);

                // EconomyResponse.transactionSuccess()
                transactionSuccessMethod = economyResponseClass.getMethod("transactionSuccess");

                isVaultLoaded = (econ != null);
            }
        } catch (Exception | NoClassDefFoundError e) {
            // Bukkit/Vaultが見つからない場合は単に無効化する
            isVaultLoaded = false;
            econ = null;
        }
    }

    public static boolean hasEnough(ServerPlayer player, double amount) {
        if (!isVaultLoaded || econ == null || amount <= 0) return true;
        try {
            // BukkitのPlayerオブジェクトを取得
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