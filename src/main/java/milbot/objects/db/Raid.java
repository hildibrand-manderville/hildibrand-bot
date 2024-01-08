package milbot.objects.db;

import milbot.chatapi.ChatFormatter;
import milbot.chatapi.User;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.Instance;
import milbot.objects.RaidSettings;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "raid")
public class Raid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "backendId")
    public GuildSettings guild;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "backendId")
    public UserSettings creator;

    public int runid;
    public String lodestoneid;
    public String name;
    public String description;
    public long created;
    public long time;
    public boolean noSprout;
    public boolean noMulti;
    public long messageid;
    public long channelid;
    public boolean isLocked;
    public boolean isUndersized;
    public boolean isMIL;
    public boolean isNoEcho;
    public boolean isNotified;
    @ColumnDefault("false")
    public boolean isVoting = false;
    public int hardCap = 99;
    public boolean isAutoLocked;
    @ColumnDefault("0")
    public long threadid = 0;
    @ColumnDefault("0")
    public long votingMaxNum = 0;
    @ColumnDefault("0")
    public long lengthInHrs = 0;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "raid_id", referencedColumnName = "id")
    private List<RaidMember> members = new ArrayList<>();

    protected Raid() {
    }

    public Raid(int runid, GuildSettings gs, UserSettings creator, String lodestoneid, String name, String description, long time, boolean noSprout, boolean noMulti) {
        this.lodestoneid = lodestoneid;
        this.creator = creator;
        this.runid = runid;
        this.name = name;
        this.time = time;
        this.noSprout = noSprout;
        this.noMulti = noMulti;
        this.created = System.currentTimeMillis();
        this.guild = gs;
        setDescriptionParsed(description);
    }

    public boolean hasUser(User user) {
        return hasUser(user.getId());
    }

    public boolean hasUser(long id) {
        for (RaidMember r : members) {
            if (r.user.backendId == id) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addMember(RaidMember r) {
        members.add(r);
    }

    public synchronized void removeMember(UserSettings user) {
        members.removeIf(rm -> rm.user.backendId == user.backendId);
    }

    public synchronized RaidMember getMember(User user) {
        return getMember(user.getId());
    }

    public synchronized RaidMember getMember(long id) {
        for (RaidMember r : members) {
            if (r.user.backendId == id) {
                return r;
            }
        }
        return null;
    }

    public synchronized RaidMember[] getMembers() {
        return members.toArray(new RaidMember[0]);
    }

    public synchronized int getMemberCount() {
        return members.size();
    }

    public void applyRaidSettings(RaidSettings rs) {
        isUndersized = rs.unsync;
        isMIL = rs.mil;
        isNoEcho = rs.noecho;
        isNotified = rs.noNotify;
        noSprout = rs.noSprout;
        noMulti = rs.noMulti;
        isVoting = rs.voting;
    }

    public void applyRaidSettings(Raid r) {
        isUndersized = r.isUndersized;
        isMIL = r.isMIL;
        isNoEcho = r.isNoEcho;
        isNotified = false;
        noSprout = r.noSprout;
        noMulti = r.noMulti;
        hardCap = r.hardCap;
        votingMaxNum = r.votingMaxNum;
        isVoting = r.isVoting;
    }

    public void setDescriptionParsed(String desc) {
        if (desc != null) {
            description = desc.replace("\\n", "\n");
        } else {
            description = null;
        }
    }

    public synchronized void removeAllMembers() {
        members.clear();
    }

    public synchronized void shoveMemberDown(RaidMember rm) {
        members.remove(rm);
        members.add(rm);
    }

    public String getNameWithModifierEmojis(ChatFormatter cf) {
        StringBuilder sb = new StringBuilder();
        sb.append(cf.getInBold("[#" + runid + "] "));
        String name = this.name;
        if (lodestoneid != null) {
            Instance i = FF14GameDatabase.getInstanceByLodestoneId(lodestoneid);
            if (i != null) {
                if (!i.altnames.isEmpty()) {
                    name = i.altnames.get(0);
                }
            }
        }
        if (name.length() > 16) {
            name = name.substring(0, 16) + "...";
        }
        sb.append(name);
        if (isUndersized) {
            sb.append(cf.getIcon(guild.getServer(), "DFSettingUndersized"));
        }
        if (isMIL) {
            sb.append(cf.getIcon(guild.getServer(), "DFSettingMIL"));
        }
        if (isNoEcho) {
            sb.append(cf.getIcon(guild.getServer(), "DFSettingNoEcho"));
        }
        return sb.toString();
    }
}