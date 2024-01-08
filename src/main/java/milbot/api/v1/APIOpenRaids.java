package milbot.api.v1;

import eu.haruka.jpsfw.web.Webserver;
import milbot.api.APIBase;
import milbot.api.HildiAPI;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.db.FF14Class;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;
import milbot.objects.db.RaidMember;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class APIOpenRaids extends HildiAPI<APIOpenRaids.RaidRequest, APIOpenRaids.RaidResponse> {

    public APIOpenRaids(Webserver server) {
        super(server);
    }

    @Override
    protected Class<RaidRequest> getInputType() {
        return RaidRequest.class;
    }

    @Override
    protected RaidResponse handle(RaidRequest in, HttpServletRequest req, HttpServletResponse resp) {
        GuildSettings gs = FF14GameDatabase.getGuild(in.server_id);
        if (gs == null) {
            throw new IllegalArgumentException("invalid server id");
        }
        RaidResponse rr = new RaidResponse();
        for (Raid r : gs.getOpenRaids()) {
            rr.active_raids.add(new RaidEntry(r));
        }

        return rr;
    }

    public static class RaidRequest extends APIBase {
        public long server_id;
    }

    public static class RaidResponse extends APIBase {

        public RaidResponse() {
            status = true;
        }

        public List<RaidEntry> active_raids = new ArrayList<>();
    }

    public static class RaidEntry {
        public int run_id;
        public String lodestone_id;
        public String name;
        public String description;
        public long created;
        public long raid_time;
        public boolean no_sprout;
        public boolean no_multi_group;
        public boolean is_locked;
        public boolean is_evil;
        public boolean is_mil;
        public boolean is_no_echo;
        public boolean is_notified;
        public boolean is_voting_enabled;
        public int player_cap;
        public boolean is_auto_locked;
        public long voting_options;
        public long length_in_hours;
        public List<RaidEntryMember> members;

        public RaidEntry(Raid r) {
            run_id = r.runid;
            lodestone_id = r.lodestoneid;
            name = r.name;
            description = r.description;
            created = r.created;
            raid_time = r.time;
            no_sprout = r.noSprout;
            no_multi_group = r.noMulti;
            is_locked = r.isLocked;
            is_evil = r.isUndersized;
            is_mil = r.isMIL;
            is_no_echo = r.isNoEcho;
            is_notified = r.isNotified;
            is_voting_enabled = r.isVoting;
            player_cap = r.hardCap;
            is_auto_locked = r.isAutoLocked;
            voting_options = r.votingMaxNum;
            length_in_hours = r.lengthInHrs;
            members = new ArrayList<>();
            for (RaidMember rm : r.getMembers()) {
                members.add(new RaidEntryMember(rm));
            }
        }
    }

    public static class RaidEntryMember {
        public long id;
        public String name;
        public int raid_count;
        public boolean is_recording;
        public List<FF14ClassEntry> classes;
        public int join_status;
        public boolean is_cloned;

        public RaidEntryMember(RaidMember rm) {
            id = rm.user.backendId;
            name = rm.user.nickname;
            raid_count = rm.user.raidcount;
            is_recording = rm.user.camera;
            join_status = 1;
            if (rm.isBackup) {
                join_status = 2;
            } else if (rm.isMaybe) {
                join_status = 3;
            }
            is_cloned = rm.isClonedEntry;
            classes = new ArrayList<>();
            for (FF14Class c : rm.classes) {
                classes.add(new FF14ClassEntry(c));
            }
        }
    }

    public static class FF14ClassEntry {
        public int id;
        public String name;
        public String name_short;
        public String category;

        public FF14ClassEntry(FF14Class c) {
            id = c.id;
            name = c.name;
            name_short = c.shortname;
            category = c.category;
        }
    }
}
