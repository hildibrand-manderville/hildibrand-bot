package milbot.util;

import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.utils.Span;
import eu.haruka.jpsfw.logging.GlobalLogger;
import milbot.Main;
import milbot.chatapi.ChatAPI;
import milbot.chatapi.ChatFormatter;
import milbot.lodestone.FF14GameDatabase;
import net.dv8tion.jda.api.JDAInfo;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static List<String> discordArgParse(String in) {
        ArrayList<String> args = new ArrayList<>();
        String[] arr = in.replace(',', ' ').split(" ");
        for (String s : arr) {
            if (!s.isBlank()) {
                args.add(s.trim());
            }
        }
        return args;
    }

    public static long hoursToMs(long hrs) {
        return hrs * 60 * 60 * 1000;
    }

    public static void logElapsedTime(long start) {
        GlobalLogger.finer("Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
    }

    public static boolean isNDaysOldOrOlder(long date, int days) {
        return date + hoursToMs(days * 24L) < System.currentTimeMillis();
    }

    public static long minutesToMs(long min) {
        return min * 60 * 1000;
    }

    public static Long parseTime(String timeStr) {
        Span parsedtime = null;
        try {
            parsedtime = Chronic.parse(timeStr);
        } catch (Exception ex) {
            GlobalLogger.exception(ex, "Failed to parse time: " + timeStr);
        }
        if (parsedtime == null) {
            return null;
        }
        return parsedtime.getEnd();
    }

    public static String getInfoString(ChatAPI<?> api) {
        ChatFormatter<?> cf = api.getChatFormatter();
        return cf.getInBold(Main.APPLICATION_NAME + " " + Main.APPLICATION_VERSION) + "\nFinal Fantasy 14 raid scheduler and group planner\n\nJDA: " + JDAInfo.VERSION + "\nBot Help: " + cf.getURL("https://github.com/hildibrand-manderville/mil-discord-bot/wiki/") + "\nDeveloper: Mitsuhide#6002\n\nNot affiliated with Discord or TeamSpeak.\n(C) SQUARE ENIX CO., LTD. All Rights Reserved. FINAL FANTASY is a registered trademark of Square Enix Holdings Co., Ltd. All material used under license.\n\nIt is important to enjoy the game.\n\n" + cf.getInBold("Bot Instance Info:") + "\nInstance Owner: " + Main.config.settings.owner + "\nDatabase Backend: " + FF14GameDatabase.getVersionInfo() + "\nSelected Chat API: " + Main.config.chat_api;
    }
}
