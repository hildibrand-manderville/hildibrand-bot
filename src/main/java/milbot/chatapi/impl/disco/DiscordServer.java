package milbot.chatapi.impl.disco;

import eu.haruka.jpsfw.logging.GlobalLogger;
import eu.haruka.jpsfw.util.Downloader;
import milbot.Main;
import milbot.chatapi.CommandParameter;
import milbot.chatapi.PermissionLevel;
import milbot.chatapi.Server;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.RaidListMessage;
import milbot.objects.RaidMessage;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;
import milbot.raid.MessageGenerator;
import milbot.util.IconGenerator;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DiscordServer implements Server {

    private DiscordAPI api;
    private Guild g;
    private ArrayList<DiscordCommandSetupEntry> commandSetup;

    DiscordServer(DiscordAPI api, Guild g) {
        this.api = api;
        if (g == null) {
            throw new NullPointerException();
        }
        this.g = g;
        this.commandSetup = new ArrayList<>();
    }

    @Override
    public long getId() {
        return g.getIdLong();
    }

    @Override
    public String getName() {
        return g.getName();
    }

    public Guild getDiscordGuild() {
        return g;
    }

    @Override
    public void registerCommand(String command, String description, PermissionLevel level, CommandParameter... parameters) {

        SlashCommandData cmd;
        SubcommandData sd = null;

        String[] subcommands = command.split("/");

        DiscordCommandSetupEntry se = null;
        for (DiscordCommandSetupEntry e : commandSetup) {
            if (e.commandString.equals(subcommands[0])) {
                se = e;
                break;
            }
        }
        if (se == null) {
            se = new DiscordCommandSetupEntry();
        }

        if (subcommands.length > 1) { // todo: multi-slash commands? = (subcommand groups?)
            if (se.command != null) {
                cmd = se.command;
            } else {
                cmd = Commands.slash(subcommands[0], description);
            }
            sd = new SubcommandData(subcommands[1], description);
            cmd.addSubcommands(sd);
            command = subcommands[0];
        } else {
            cmd = Commands.slash(command, description);
        }

        if (level != PermissionLevel.EVERYONE) {
            cmd.setDefaultPermissions(DefaultMemberPermissions.DISABLED);
        }
        for (CommandParameter p : parameters) {
            OptionType ot;
            switch (p.getType()) {
                case INT:
                    ot = OptionType.INTEGER;
                    break;
                case STRING:
                    ot = OptionType.STRING;
                    break;
                case BOOL:
                    ot = OptionType.BOOLEAN;
                    break;
                case USER:
                    ot = OptionType.USER;
                    break;
                case GROUP:
                    ot = OptionType.ROLE;
                    break;
                case ROOM:
                    ot = OptionType.CHANNEL;
                    break;
                default:
                    throw new IllegalArgumentException("unknown option type: " + p.getType());
            }
            OptionData op = new OptionData(ot, p.getName(), p.getDescription(), p.isRequired());

            if (sd != null) {
                sd.addOptions(op);
            } else {
                cmd.addOptions(op);
            }
        }

        Consumer<Command> success = null;
        if (level == PermissionLevel.CREATE) {
            success = this::lateRegisterCommandCreator;
        } else if (level == PermissionLevel.RAID_LEADER) {
            success = this::lateRegisterCommandRaidLeader;
        } else if (level == PermissionLevel.ADMIN) {
            success = this::lateRegisterCommandAdmin;
        }

        se.commandString = command;
        se.command = cmd;
        se.success = success;
        if (!commandSetup.contains(se)) {
            commandSetup.add(se);
        }
    }

    @Override
    public void commitCommands() {
        for (DiscordCommandSetupEntry se : commandSetup) {
            GlobalLogger.fine("[discord] Registering command: " + se.commandString + ", " + se.command.getSubcommands().size() + " subs");
            g.upsertCommand(se.command).queue(se.success);
        }
    }

    @Override
    public void editMessage(Raid raid, RaidMessage rm) {
        TextChannel t = g.getTextChannelById(raid.channelid);
        if (t == null) {
            GlobalLogger.severe("[discord] Can't recreate raid message for guild " + g + " in channel " + raid.channelid + " - channel doesn't exist");
            return;
        }
        t.editMessageById(raid.messageid, api.editRaidMessageDiscordObject(this, rm, false)).queue();
    }

    @Override
    public void editMessage(long roomid, long messageid, RaidListMessage rm) {
        TextChannel t = g.getTextChannelById(roomid);
        if (t == null) {
            GlobalLogger.severe("[discord] Can't recreate raid list message for guild " + g + " in channel " + roomid + " - channel doesn't exist");
            return;
        }
        t.editMessageById(messageid, api.editRaidListMessageDiscordObject(this, rm)).queue();
    }

    @Override
    public void postMessage(long roomid, RaidListMessage rm, Consumer<Long> success) {
        TextChannel t = g.getTextChannelById(roomid);
        if (t == null) {
            GlobalLogger.severe("[discord] Can't post raid list message for guild " + g + " in channel " + roomid + " - channel doesn't exist");
            return;
        }
        t.sendMessage(api.buildRaidListMessageDiscordObject(this, rm)).queue(message -> success.accept(message.getIdLong()));
    }

    @Override
    public void deleteMessage(Raid raid) {
        TextChannel t = g.getTextChannelById(raid.channelid);
        if (t == null) {
            GlobalLogger.severe("[discord] Can't delete raid message for guild " + g + " in channel " + raid.channelid + " - channel doesn't exist");
            return;
        }
        t.deleteMessageById(raid.messageid).queue();
    }

    @Override
    public void setServerIcon(GuildSettings gs, int raidnum) throws IOException {
        File server_image = new File(Main.CACHE_DIR, String.valueOf(getId()));
        if (!server_image.exists()) {
            String icon = g.getIconUrl();
            if (icon != null) {
                Downloader d = new Downloader(icon);
                d.downloadToFile(server_image);
            } else {
                GlobalLogger.warning("Attempted setServerIcon while there's no server icon! Disabling feature.");
                gs.updateServerIcon = false;
                FF14GameDatabase.save(gs);
                return;
            }
        }

        BufferedImage bf = IconGenerator.writeRaidNumberOnImage(server_image, raidnum);

        File target_image = new File(Main.CACHE_DIR, getId() + "_conv");
        ImageIO.write(bf, "png", target_image);
        g.getManager().setIcon(Icon.from(target_image)).queue();

    }

    private void lateRegisterCommandCreator(Command c) {
        long id = c.getIdLong();

        GuildSettings gs = getGuildSettings();
        if (!gs.createCommandIds.contains(id)) {
            gs.createCommandIds.add(id);
        }
        updateAllCommandPermissions(Collections.singletonList(id), gs.createRoleId);
        FF14GameDatabase.save(gs);
    }

    private void lateRegisterCommandRaidLeader(Command c) {
        long id = c.getIdLong();
        GuildSettings gs = getGuildSettings();
        if (!gs.raidLeaderCommandIds.contains(id)) {
            gs.raidLeaderCommandIds.add(id);
        }
        updateAllCommandPermissions(Collections.singletonList(id), gs.raidLeaderRoleId);
        FF14GameDatabase.save(gs);
    }

    private void lateRegisterCommandAdmin(Command c) {
        long id = c.getIdLong();
        GuildSettings gs = getGuildSettings();
        if (!gs.administratorCommandIds.contains(id)) {
            gs.administratorCommandIds.add(id);
        }
        updateAllCommandPermissions(Collections.singletonList(id), gs.administratorRoleId);
        FF14GameDatabase.save(gs);
    }

    public void updateCommandPermissions() {
        GuildSettings gs = getGuildSettings();
        updateAllCommandPermissions(gs.administratorCommandIds, gs.administratorRoleId);
        updateAllCommandPermissions(gs.raidLeaderCommandIds, gs.administratorRoleId, gs.raidLeaderRoleId);
        updateAllCommandPermissions(gs.createCommandIds, gs.administratorRoleId, gs.raidLeaderRoleId, gs.createRoleId);
    }

    private void updateAllCommandPermissions(List<Long> commands, long... allowable_roles) {
        /*
        TODO: this appears to have been broken by discord:
        [14:15:56][SEVERE] net.dv8tion.jda.api.requests.RestAction - RestAction queue returned failure: [ErrorResponseException] 20001: Bots cannot use this endpoint
         */
        /*for (long command : commands) {
            ArrayList<CommandPrivilege> privileges = new ArrayList<>();
            privileges.add(CommandPrivilege.enableUser(g.getOwnerIdLong()));
            for (long role : allowable_roles) {
                if (role > 0) {
                    privileges.add(CommandPrivilege.enableRole(role));
                }
            }
            g.updateCommandPrivilegesById(command, privileges).queue();
        }*/
    }

    public void recreateRaidMessage(Raid raid) {
        editMessage(raid, MessageGenerator.generateRaidPost(api, this, raid));
    }
}
