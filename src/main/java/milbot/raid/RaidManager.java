package milbot.raid;

import milbot.chatapi.User;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.Instance;
import milbot.objects.db.*;

import java.util.List;

public class RaidManager {
    public synchronized static Raid create(GuildSettings gs, UserSettings creator, Instance i, String desc, long time, boolean noSprout, boolean noMulti) {
        int raidid = ++gs.nextRunId;
        Raid r = new Raid(raidid, gs, creator, i.lsid, i.name, desc, time, noSprout, noMulti);
        gs.addRaid(r);
        FF14GameDatabase.save(gs);
        return r;
    }

    // copy
    public synchronized static Raid create(GuildSettings gs, Raid r) {
        r.runid = ++gs.nextRunId;
        gs.addRaid(r);
        FF14GameDatabase.save(gs);
        return r;
    }

    public synchronized static RaidMember join(Raid raid, User u, List<FF14Class> classes, boolean isMaybe, boolean isBackup) {
        UserSettings user = FF14GameDatabase.getOrCreateUser(u);
        return RaidManager.join(raid, user, classes, isMaybe, isBackup, false);
    }

    public synchronized static RaidMember join(Raid raid, UserSettings user, List<FF14Class> classes, boolean isMaybe, boolean isBackup, boolean isCloned) {
        if (classes.isEmpty()) {
            if (user.savedClasses != null && !user.savedClasses.isBlank()) {
                classes = FF14GameDatabase.parseClassesFromString(user.savedClasses);
            }
        }
        RaidMember rm = new RaidMember(user, raid, isMaybe, isBackup, classes);
        rm.isClonedEntry = isCloned;
        raid.addMember(rm);
        user.raidcount++;
        FF14GameDatabase.save(raid);
        FF14GameDatabase.save(user);
        return rm;
    }

    public synchronized static void leave(Raid raid, UserSettings user) {
        raid.removeMember(user);
        user.raidcount--;
        FF14GameDatabase.save(raid);
        FF14GameDatabase.save(user);
    }

}
