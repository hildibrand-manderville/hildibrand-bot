package milbot.chatapi;

import milbot.lodestone.FF14GameDatabase;
import milbot.objects.RaidListMessage;
import milbot.objects.RaidMessage;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * This class represents a server inside a chat service.
 * Servers can be either closed communities or public sections.
 */
public interface Server {

    /**
     * The id of the server.
     *
     * @return id of the server.
     */
    public long getId();

    /**
     * The name of the server.
     *
     * @return name of the server.s
     */
    public String getName();

    /**
     * Returns the equivalent GuildSettings (database) object for this server.
     *
     * @return the database information for this server.
     * @see FF14GameDatabase
     */
    public default GuildSettings getGuildSettings() {
        return FF14GameDatabase.getOrCreateGuild(this);
    }

    /**
     * Registers a command to this server. Once registered, a server should handle chat commands, such as /join, /leave, etc. This function should be called only once per server, unless --reinitialize is specified in startup.
     *
     * @param command     The command name. This argument MAY contain one or multiple slashes to allow for command trees, ex: option/notification/raid-create
     * @param description The user-facing description for this command.
     * @param level       The minimum required permission for this command.
     * @param parameters  An array of CommandParameters, determining arguments for the command.
     */
    public void registerCommand(String command, String description, PermissionLevel level, CommandParameter... parameters);

    public void commitCommands();

    public void editMessage(Raid raid, RaidMessage rm);

    public void editMessage(long roomid, long messageid, RaidListMessage rm);

    public void postMessage(long roomid, RaidListMessage rm, Consumer<Long> success);

    public void deleteMessage(Raid raid);

    public void setServerIcon(GuildSettings gs, int raidnum) throws IOException;
}
