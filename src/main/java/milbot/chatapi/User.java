package milbot.chatapi;

import milbot.lodestone.FF14GameDatabase;
import milbot.objects.db.UserSettings;

import java.util.function.Consumer;

/**
 * This class represents a single user in the chat system.
 */
public interface User {

    /**
     * Returns the id of the user.
     *
     * @return the id of the user.
     */
    public long getId();

    /**
     * Returns the name of the user.
     *
     * @return the name of the user.
     */
    public String getName();

    /**
     * Returns the equivalent UserSettings (database) object for this user.
     *
     * @return the database information for this user.
     * @see FF14GameDatabase
     */
    public default UserSettings getUserSettings() {
        return FF14GameDatabase.getOrCreateUser(this);
    }

    /**
     * Returns or fetches the user's nickname for the given server. This function may be called multiple times, even before success is fired.
     *
     * @param server  The server from which the nickname should be retrieved from.
     * @param success A function that will be called when the nickname was successfully retrieved, or null.
     * @return The nickname for the user, or a loading indicator.
     * @apiNote Honestly, this is just such a weird function because of discord ~sigh~
     */
    public String getEffectiveName(Server server, Consumer<String> success);

}
