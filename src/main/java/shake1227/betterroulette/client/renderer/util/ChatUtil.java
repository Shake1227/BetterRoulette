package shake1227.betterroulette.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtil {
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");

    public static MutableComponent parse(String text) {
        MutableComponent component = Component.literal("");
        String[] parts = text.split("(?i)(?=&[0-9a-fk-or])");
        for (String part : parts) {
            Matcher matcher = FORMATTING_CODE_PATTERN.matcher(part);
            if (matcher.find()) {
                ChatFormatting format = ChatFormatting.getByCode(matcher.group(1).toLowerCase().charAt(0));
                String content = part.substring(matcher.end());
                if (format != null) {
                    component.append(Component.literal(content).withStyle(format));
                }
            } else {
                component.append(Component.literal(part));
            }
        }
        return component;
    }
}