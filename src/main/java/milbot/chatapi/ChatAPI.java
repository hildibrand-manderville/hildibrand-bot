package milbot.chatapi;

import milbot.Config;
import milbot.objects.db.Raid;

import java.util.List;

/**
 * Class defining a chatting API service for the bot. (Matrix, Discord, Guilded, .....)
 */
public interface ChatAPI<ServerType extends Server> {

    /**
     * Fetches the user with the given id from the chat service.
     *
     * @param id The user id.
     * @return The user object or null if not found.
     */
    public User getUserById(long id);

    /**
     * Fetches the server/community/guild with the given id from the chat service.
     *
     * @param id The server id.
     * @return The server object, null if not found, null if the chat service does not use servers.
     */
    public ServerType getServerById(long id);

    /**
     * Initializes the chat service. This function may block while requests are being run.
     *
     * @param config The current bot configuration.
     * @param argl   The startup arguments.
     * @throws Exception If something goes wrong :p
     */
    public void start(Config config, List<String> argl) throws Exception;

    /**
     * Returns the chat formatter for this chat API.
     *
     * @return the chat formatter for this chat API.
     */
    public ChatFormatter<ServerType> getChatFormatter();

    public void sendDMTo(long backendId, String message, String raidUrl);

    public String buildURLToRaidPost(long serverId, long channelId, long messageId);

    public void checkThreadState(Raid r, long threadid);
}
