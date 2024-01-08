package milbot.chatapi;

import milbot.chatapi.impl.disco.DiscordServer;
import milbot.objects.db.FF14Class;

/**
 * Class handling chat formatting operations such as icons or links.
 */
public interface ChatFormatter<ServerType extends Server> {

    /**
     * Converts an icon code to an actual icon. For example, this will be used to convert emoji names to their respective icon.
     *
     * @param server The server the operation is taking place in.
     * @param code   The icon code to convert.
     * @return The converted icon, or the input parameter, if the icon could not be converted.
     */
    public String getIcon(ServerType server, String code);

    /**
     * Converts an icon code to an actual icon. For example, this will be used to convert emoji names to their respective icon.
     *
     * @param api      The ChatAPI from which the server should be retrieved from.
     * @param serverId The server id.
     * @param code     The icon code to convert.
     * @return The converted icon, or the input parameter, if the icon could not be converted.
     */
    public String getIcon(ChatAPI<? extends DiscordServer> api, long serverId, String code);

    public default String getIcon(ServerType server, FF14Class clazz) {
        return getIcon(server, clazz.shortname);
    }

    public String getInItalic(String str);

    public String getInBold(String str);

    public String getUnderlined(String str);

    public String getTimeString(long time);

    public String getGroup(long groupId);

    public String getURL(String url);

    public String getRelativeTimeString(long time);
}
