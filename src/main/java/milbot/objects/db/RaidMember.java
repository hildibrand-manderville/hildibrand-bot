package milbot.objects.db;

import milbot.lodestone.FF14GameDatabase;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "raid_member")
public class RaidMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @ManyToOne
    public Raid raid;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "backendId")
    public UserSettings user;

    public boolean isMaybe;
    public boolean isBackup;
    @ColumnDefault("false")
    public boolean isClonedEntry = false;

    @ManyToMany
    @JoinTable(
            name = "raid_classes",
            joinColumns = @JoinColumn(name = "raid_member_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "class_id", referencedColumnName = "id")
    )
    public List<FF14Class> classes;

    protected RaidMember() {
    }

    public RaidMember(UserSettings user, Raid raid, boolean isMaybe, boolean isBackup, List<FF14Class> classes) {
        this.user = user;
        this.raid = raid;
        this.isMaybe = isMaybe;
        this.isBackup = isBackup;
        this.classes = new ArrayList<>(classes);
    }

    public String getParticipationEmoji(boolean isLeader, boolean isAlliance, int groupno) {
        if (isBackup) {
            return "PlayerHire";
        } else if (isMaybe) {
            return "FF14Question";
        } else if (isLeader && isAlliance && groupno == 1) {
            return "PlayerAllianceLeader";
        } else if (isLeader && isAlliance) {
            return "PlayerAlliancePartyLeader";
        } else if (isLeader) {
            return "PlayerLeader";
        } else if (isAlliance) {
            return "PlayerAllianceMember";
        } else {
            return "PlayerMember";
        }
    }

    public void setJoinState(boolean maybe, boolean backup) {
        isMaybe = maybe;
        isBackup = backup;
        FF14GameDatabase.save(this);
    }

    public boolean hasClass(String classname) {
        for (FF14Class c : classes) {
            if (c.shortname.equals(classname)) {
                return true;
            }
        }
        return false;
    }
}
