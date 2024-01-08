package milbot;

import com.google.gson.reflect.TypeToken;
import eu.haruka.jpsfw.configuration.Json;
import eu.haruka.jpsfw.configuration.JsonT;
import eu.haruka.jpsfw.logging.GlobalLogger;
import eu.haruka.jpsfw.web.Webserver;
import milbot.api.API404;
import milbot.api.v1.APIOpenRaids;
import milbot.api.v1.APIStatus;
import milbot.chatapi.ChatAPI;
import milbot.chatapi.Server;
import milbot.chatapi.impl.disco.DiscordAPI;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.Instance;
import milbot.objects.db.FF14Class;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;
import milbot.raid.TimedNotifications;
import milbot.util.HibernateUtil;
import milbot.util.Util;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static final String APPLICATION_NAME = "Hildibrand Manderville";
    public static final String APPLICATION_VERSION = "2.4";

    public static final File CACHE_DIR = new File("cache");
    public static final File CONFIG_DIR = new File("config");
    public static final File DATA_DIR = new File("data");
    public static final File BACKUP_DIR = new File(DATA_DIR, "backups");

    public static Config config;
    public static List<FF14Class> classes;
    public static List<Instance> instances;
    private static DiscordAPI discordAPI;
    public static boolean running;

    public static void main(String[] args) throws Exception {
        List<String> argl = Arrays.asList(args);
        if (!CACHE_DIR.exists()) {
            Files.createDirectory(CACHE_DIR.toPath());
        }
        if (!CONFIG_DIR.exists()) {
            Files.createDirectory(CONFIG_DIR.toPath());
        }
        if (!DATA_DIR.exists()) {
            Files.createDirectory(DATA_DIR.toPath());
        }
        if (!BACKUP_DIR.exists()) {
            Files.createDirectory(BACKUP_DIR.toPath());
        }

        if (!Config.CONFIG_FILE.exists()) {
            new JsonT<>(new Config()).save(Config.CONFIG_FILE);
            System.out.println("Config file generated, edit then try again!");
            return;
        }
        GlobalLogger.initialize();

        File cfile = Config.CONFIG_FILE;
        if (argl.contains("--config")) {
            cfile = new File(CONFIG_DIR, argl.get(argl.indexOf("--config") + 1));
            GlobalLogger.info("Overriding config file with: " + cfile);
        }

        config = new JsonT<>(cfile, Config.class).getObject();

        Main.running = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Main.running = false));

        GlobalLogger.info(APPLICATION_NAME + " " + APPLICATION_VERSION);

        GlobalLogger.info("Connecting to database...");
        HibernateUtil.getSessionFactory();

        if (argl.contains("--dbwebui") || config.api.enable) {
            Webserver ws = new Webserver(true, config.api.port, null, null);
            ws.addHandler(new APIStatus(ws), "/api/hildi-v1/status");
            ws.addHandler(new APIOpenRaids(ws), "/api/hildi-v1/raids/open");
            ws.addHandler(new API404(ws), "/api/*");
            ws.start();
        }

        GlobalLogger.fine("Loading classes...");
        classes = new ArrayList<>();
        if (argl.contains("--initclasses") || FF14GameDatabase.getClass("TANK") == null) {
            FF14GameDatabase.initializeClasses();
        }

        GlobalLogger.fine("Loading duties...");
        File instancesFile = new File(DATA_DIR, "instances.json");
        if (Util.isNDaysOldOrOlder(instancesFile.lastModified(), 30 * 6)) {
            GlobalLogger.warning("Raid instance information file is older than 6 months, consider running the lodestone scraper.");
        }
        //noinspection unchecked
        instances = (List<Instance>) new Json(instancesFile, new TypeToken<ArrayList<Instance>>() {
        }).getObject();

        if (argl.contains("--no-connect")) {
            GlobalLogger.info("--no-connect specified.");
            return;
        }


        if (config.chat_api.equals("discord")) {
            if (config.discord.token == null || config.discord.token.isEmpty()) {
                GlobalLogger.severe("No token set!");
                return;
            }

            discordAPI = new DiscordAPI();
            discordAPI.start(config, argl);
        } else {
            throw new IllegalArgumentException("Unknown chat api in config: " + config.chat_api);
        }


        GlobalLogger.info("Application active and running!");

        for (GuildSettings gs : FF14GameDatabase.getAllServers()) {
            for (Raid r : gs.getRaids()) {
                if (Util.isNDaysOldOrOlder(r.created, config.settings.old_raid_purge_days)) {
                    GlobalLogger.fine("Deleting raid " + r.runid + " from guild " + gs.backendId);
                    gs.removeRaid(r);
                    FF14GameDatabase.save(gs);
                }
            }
        }

        new TimedNotifications().start();

    }

    public static ChatAPI<? extends Server> getChatAPI() {
        if (discordAPI != null) {
            return discordAPI;
        } else {
            throw new IllegalStateException("all chatapis are null");
        }
    }

}
