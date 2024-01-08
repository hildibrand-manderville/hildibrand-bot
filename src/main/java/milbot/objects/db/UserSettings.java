package milbot.objects.db;

import milbot.Main;
import milbot.chatapi.Server;
import milbot.chatapi.User;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.function.Consumer;

@Entity
@Table(name = "player")
public class UserSettings {

    @Id
    public long backendId;
    public boolean camera = false;
    public boolean notifications = true;
    public int raidcount = 0;
    @ColumnDefault("0")
    public int lateLeaveCount = 0;
    public String nickname;
    public String savedClasses;
    private transient boolean fetching;

    protected UserSettings() {
    }

    public UserSettings(long userid) {
        this();
        backendId = userid;
    }

    public String getRealName(Server server) {
        return getRealName(server, null);
    }

    public String getRealName(Server server, Consumer<String> success) {
        User u = Main.getChatAPI().getUserById(backendId);
        if (u != null) {
            return u.getEffectiveName(server, success);
        } else {
            return Main.getChatAPI().getChatFormatter().getInItalic("unknown user");
        }
    }

    @Override
    public String toString() {
        return "UserSettings{" +
                "backendId=" + backendId +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}