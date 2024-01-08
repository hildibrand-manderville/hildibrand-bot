package milbot.chatapi.impl.disco;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.function.Consumer;

public class DiscordCommandSetupEntry {

    public String commandString;
    public SlashCommandData command;
    public Consumer<Command> success;

    public DiscordCommandSetupEntry() {
    }
}
