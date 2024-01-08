package milbot.chatapi.impl.disco;

import eu.haruka.jpsfw.logging.GlobalLogger;
import milbot.chatapi.Server;
import milbot.chatapi.User;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.db.UserSettings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.function.Consumer;

public class DiscordUser implements User {

    private DiscordAPI api;
    private net.dv8tion.jda.api.entities.User u;

    private boolean fetching;

    DiscordUser(DiscordAPI api, net.dv8tion.jda.api.entities.User u) {
        this.api = api;
        if (u == null) {
            throw new IllegalArgumentException("trying to create DiscordUser with null JDA user");
        }
        this.u = u;
    }

    @Override
    public long getId() {
        return u.getIdLong();
    }

    @Override
    public String getName() {
        return u.getName();
    }

    @Override
    public String getEffectiveName(Server server, Consumer<String> success) {
        UserSettings us = FF14GameDatabase.getOrCreateUser(this);
        if (us.nickname == null) {
            Guild g = api.getServerById(server.getId()).getDiscordGuild();
            long backendId = getId();
            Member m = g.getMemberById(backendId);
            if (m == null) {
                if (!fetching) {
                    fetching = true;
                    GlobalLogger.warning("[discord] No member found for " + backendId + ", querying discord!");
                    g.retrieveMemberById(backendId).queue(member1 -> {
                        GlobalLogger.info("[discord] Updated name in guild " + g + " of user " + backendId + " to: " + member1.getEffectiveName());
                        us.nickname = member1.getEffectiveName();
                        fetching = false;
                        FF14GameDatabase.save(us);
                        if (success != null) {
                            success.accept(us.nickname);
                        }
                    });
                }
                return api.getChatFormatter().getInItalic("Loading...");
            } else {
                us.nickname = m.getEffectiveName();
            }
        }
        return us.nickname;
    }
}
