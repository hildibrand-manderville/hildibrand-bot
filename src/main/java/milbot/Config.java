package milbot;

import java.io.File;

public class Config {

    public static final File CONFIG_FILE = new File(Main.CONFIG_DIR, "config.json");

    public String chat_api = "discord";

    public Discord discord = new Discord();
    public Icons icons = new Icons();
    public Settings settings = new Settings();
    public API api = new API();

    public class Discord {
        public String token = "";
        public String status = "with party members";
        public String status_type = "DEFAULT";
        public long owner_backdoor_id = 1L;
    }

    public class Icons {
        public String dungeon = "https://ffxiv.consolegameswiki.com/mediawiki/images/a/ab/Dungeon.png";
        public String trial = "https://ffxiv.consolegameswiki.com/mediawiki/images/0/0d/Trial.png";
        public String raid = "https://ffxiv.consolegameswiki.com/mediawiki/images/d/d6/Raid.png";
        public String other = "https://puu.sh/HYyVT.png";
        public String ultimate_raid = "https://puu.sh/HYyVK.pn";
    }

    public class Settings {

        public int old_raid_purge_days = 760;
        public String help_url = "https://github.com/hildibrand-manderville/mil-discord-bot/wiki/Quick-Start";
        public String owner = "not specified";
        public Timings timings = new Timings();

        public int late_leave_cap = 3;
    }

    public class Timings {
        public int icon_updater = 15;
        public int raid_list = 5;
    }

    public class API {
        public boolean enable = true;
        public int port = 9999;
    }
}
