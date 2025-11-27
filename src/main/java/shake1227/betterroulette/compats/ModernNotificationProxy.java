package shake1227.betterroulette.compats;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import shake1227.betterroulette.client.renderer.util.ChatUtil;
import shake1227.modernnotification.config.ServerConfig;
import shake1227.modernnotification.core.NotificationCategory;
import shake1227.modernnotification.core.NotificationType;
import shake1227.modernnotification.network.PacketHandler;
import shake1227.modernnotification.network.S2CNotificationPacket;
import shake1227.modernnotification.util.TextFormattingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModernNotificationProxy {
    private static final String MOD_ID = "modernnotification";
    private static boolean isLoaded;

    static {
        isLoaded = ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static void sendError(ServerPlayer player, String translationKey) {
        if (!isLoaded) return;
        try {
            sendNotification(
                    player,
                    NotificationType.LEFT,
                    NotificationCategory.FAILURE,
                    null,
                    Collections.singletonList(Component.translatable(translationKey)),
                    -1
            );
        } catch (Throwable ignored) {}
    }

    public static void sendStartMessage(ServerPlayer player, Component message) {
        if (!isLoaded) return;
        try {
            sendNotification(
                    player,
                    NotificationType.LEFT,
                    NotificationCategory.SUCCESS,
                    null,
                    Collections.singletonList(message),
                    -1
            );
        } catch (Throwable ignored) {}
    }

    public static void sendResult(ServerPlayer player, String rawRouletteName, String rawPrizeName, String prizeDesc) {
        if (!isLoaded) return;
        try {
            Component rNameComp = ChatUtil.parse(rawRouletteName);
            MutableComponent titleComp = Component.translatable("chat.betterroulette.result.title", rNameComp)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(true));

            List<Component> title = Collections.singletonList(titleComp);

            List<Component> message = new ArrayList<>();

            Component pNameComp = ChatUtil.parse(rawPrizeName);
            message.add(Component.translatable("chat.betterroulette.result.won", pNameComp));

            if (prizeDesc != null && !prizeDesc.isEmpty()) {
                String formattedDesc = insertLineBreaks(prizeDesc);
                List<Component> descLines = TextFormattingUtils.parseLegacyText(formattedDesc);
                message.addAll(descLines);
            }

            sendNotification(
                    player,
                    NotificationType.TOP_RIGHT,
                    NotificationCategory.SUCCESS,
                    title,
                    message,
                    6
            );
        } catch (Throwable ignored) {}
    }

    private static void sendNotification(ServerPlayer player, NotificationType type, NotificationCategory category, List<Component> title, List<Component> message, int duration) {
        int finalDuration = duration > 0 ? duration : ServerConfig.INSTANCE.defaultDuration.get();
        S2CNotificationPacket packet = new S2CNotificationPacket(
                type,
                category,
                title,
                message,
                finalDuration
        );
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    private static String insertLineBreaks(String text) {
        if (text == null || text.length() <= 25) return text;

        StringBuilder result = new StringBuilder();
        int length = text.length();
        int currentLineLength = 0;

        boolean inQuote = false;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == '「' || c == '『' || c == '(' || c == '[') inQuote = true;
            if (c == '」' || c == '』' || c == ')' || c == ']') inQuote = false;

            result.append(c);
            currentLineLength++;

            if (c == '\n' || (i > 0 && text.charAt(i-1) == '&' && c == 'u')) {
                currentLineLength = 0;
                continue;
            }

            if (currentLineLength >= 25) {
                boolean nextIsBadStart = (i + 1 < length) && isBadLineStart(text.charAt(i + 1));

                if (!inQuote && !nextIsBadStart) {
                    result.append("&u");
                    currentLineLength = 0;
                } else if (currentLineLength >= 35) {
                    result.append("&u");
                    currentLineLength = 0;
                }
            }
        }
        return result.toString();
    }

    private static boolean isBadLineStart(char c) {
        return "、。,.!?) ]』」".indexOf(c) >= 0;
    }
}