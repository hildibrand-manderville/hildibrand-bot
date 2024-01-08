package milbot.objects.db;

import milbot.Main;
import milbot.chatapi.Server;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table(name = "guild")
public class GuildSettings {

    @Id
    public long backendId;

    public boolean allowMaybe = true;
    public long raidLeaderRoleId = 0;
    @ElementCollection
    public List<Long> raidLeaderCommandIds = new ArrayList<>();
    public long administratorRoleId = 0;
    @ElementCollection
    public List<Long> administratorCommandIds = new ArrayList<>();
    public long createRoleId = 0;
    @ElementCollection
    public List<Long> createCommandIds = new ArrayList<>();
    public int nextRunId;
    public boolean allowUnknownRaids;
    public boolean initialized;
    public int notifyMinutes = 10;
    public int autoLockMinutes;
    @ColumnDefault("true")
    public boolean retainTogglePosition = true;
    @ColumnDefault("0")
    public long raidlistRoomId = 0;
    @ColumnDefault("0")
    public long raidlistRoomMessage = 0;
    @ColumnDefault("true")
    public boolean updateServerIcon = true;
    @ColumnDefault("0")
    public long lastRaidlistUpdate = 0;
    @ColumnDefault("0")
    public long lastIconUpdate = 0;
    @ColumnDefault("0")
    public int lastIconRaidCount = 0;
    @ColumnDefault("0")
    public long raidRoomId = 0;
    @ColumnDefault("0")
    public long notifyRoleId = 0;
    @ColumnDefault("false")
    public boolean iconOnlyForTomorrowRaids;

    @OneToMany(mappedBy = "guild", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<Raid> raidList = new ArrayList<>();

    protected GuildSettings() {
    }

    public GuildSettings(long id) {
        this.backendId = id;
    }

    public Raid findRaidByMessageId(long id) {
        for (Raid r : raidList) {
            if (r.messageid == id) {
                return r;
            }
        }
        return null;
    }

    public synchronized void addRaid(Raid r) {
        raidList.add(r);
    }

    public synchronized Raid getRaid(int runid) {
        for (Raid r : raidList) {
            if (r.runid == runid && r.guild == this) {
                return r;
            }
        }
        return null;
    }

    public synchronized Raid[] getRaids() {
        return raidList.toArray(new Raid[0]);
    }

    public synchronized void removeRaid(Raid r) {
        removeRaid(r.runid);
    }

    public synchronized void removeRaid(int id) {
        for (Iterator<Raid> iterator = raidList.iterator(); iterator.hasNext(); ) {
            Raid r = iterator.next();
            if (r.runid == id && r.guild == this) {
                iterator.remove();
            }
        }
    }

    public Server getServer() {
        return Main.getChatAPI().getServerById(backendId);
    }

    public synchronized List<Raid> getOpenRaids() {
        List<Raid> openRaids = new ArrayList<>();
        for (Raid r : getRaids()) {
            if (!r.isLocked) {
                openRaids.add(r);
            }
        }
        return openRaids;
    }
}
