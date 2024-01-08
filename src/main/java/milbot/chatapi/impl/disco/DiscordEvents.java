package milbot.chatapi.impl.disco;

import eu.haruka.jpsfw.logging.GlobalLogger;
import eu.haruka.jpsfw.logging.RefCodeMaker;
import milbot.Main;
import milbot.chatapi.Server;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.Instance;
import milbot.objects.InstanceType;
import milbot.objects.RaidPreview;
import milbot.objects.RaidSettings;
import milbot.objects.db.*;
import milbot.raid.MessageGenerator;
import milbot.raid.RaidManager;
import milbot.util.Util;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DiscordEvents extends ListenerAdapter {

    private final DiscordAPI api;
    private HashMap<Long, RaidPreview> previewCache = new HashMap<>();
    private HashMap<Long, Raid> epheremal_messages = new HashMap<>();

    public DiscordEvents(DiscordAPI api) {
        this.api = api;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            GlobalLogger.info("[discord][PM] " + event.getAuthor().getName() + ": " + event.getMessage().getContentDisplay());
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        Guild g = event.getGuild();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(api.getServer(g));
        if (!gs.initialized) {
            api.initializeGuild(g);
            gs.initialized = true;
            FF14GameDatabase.save(gs);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        long start = System.currentTimeMillis();
        String command = event.getFullCommandName();

        if (event.getGuild() == null) {
            event.reply("This command can only be used in a server.").queue();
            return;
        }

        GlobalLogger.fine("[discord] onSlashCommand: " + event.getUser().getName() + " - " + command + "[" + event.getGuild().getIdLong() + "/" + event.getUser().getIdLong() + "/" + event.getCommandString() + "]");

        api.getUser(event.getUser()); // hack

        DiscordServer s = api.getServer(event.getGuild());
        DiscordUser u = api.getUser(event.getUser());

        try {
            if (command.equals("create")) {
                handleCreateRaid(s, u, event, false);
            } else if (command.equals("join")) {
                handleJoin(s, u, event, false, false);
            } else if (command.equals("joinbackup")) {
                handleJoin(s, u, event, false, true);
            } else if (command.equals("joinmaybe")) {
                handleJoin(s, u, event, true, false);
            } else if (command.equals("leave")) {
                handleLeave(s, u, event);
            } else if (command.equals("lock")) {
                handleLock(s, event);
            } else if (command.equals("delete")) {
                handleDelete(s, event);
            } else if (command.equals("set-classes")) {
                handleSetClasses(s, u, event);
            } else if (command.equals("save-classes")) {
                handleSaveClasses(s, u, event);
            } else if (command.equals("clone")) {
                handleClone(s, u, event);
            } else if (command.equals("soption set-raid-leader-role")) {
                handleSetModeratorRole(s, event);
            } else if (command.equals("soption set-administrator-role")) {
                handleSetAdminRole(s, event);
            } else if (command.equals("soption set-creator-role")) {
                handleSetCreateRole(s, event);
            } else if (command.equals("soption maybe")) {
                handleSetMaybe(s, event);
            } else if (command.equals("soption allow-unknown-raids")) {
                handleSetUnknown(s, event);
            } else if (command.equals("soption notification-reminder")) {
                handleSetNotifyTime(s, event);
            } else if (command.equals("soption auto-lock")) {
                handleSetAutoLockTime(s, event);
            } else if (command.equals("soption toggle-retains-position")) {
                handleSetTogglePosition(s, event);
            } else if (command.equals("option camera")) {
                handleSetCamera(u, event);
            } else if (command.equals("option notifications")) {
                handleSetNotifications(u, event);
            } else if (command.equals("option character-name")) {
                handleSetCharacterName(u, event);
            } else if (command.equals("modify add")) {
                handleModifyAdd(s, event);
            } else if (command.equals("modify remove")) {
                handleModifyRemove(s, event);
            } else if (command.equals("modify edit-name")) {
                handleModifyEditName(s, event);
            } else if (command.equals("modify edit-description")) {
                handleModifyEditDesc(s, event);
            } else if (command.equals("modify edit-options")) {
                handleModifyEditOptions(s, event);
            } else if (command.equals("modify edit-time")) {
                handleModifyEditTime(s, event);
            } else if (command.equals("reinitialize")) {
                handleReinitialize(s, event);
            } else if (command.equals("version")) {
                handleVersion(event);
            } else if (command.equals("hildi")) {
                handleHildi(event);
            } else if (command.equals("soption set-raidlist-channel")) {
                handleSetRaidlistChannel(s, event);
            } else if (command.equals("soption server-icon-shows-raids")) {
                handleServerIcon(s, event);
            } else if (command.equals("soption set-role-for-notifications")) {
                handleSetNotifyRole(s, event);
            } else if (command.equals("option ping-for-raids")) {
                handleSetPingForRaids(s, u, event);
            } else if (command.equals("soption icon-for-tomorrow-raids-only")) {
                handleSetIconForTomorrowOnly(s, u, event);
            } else if (command.equals("preview")) {
                handleCreateRaid(s, u, event, true);
            } else {
                sendUserError(s, event, "Sorry, the command \"" + command + "\" is not implemented yet!");
            }
        } catch (Exception ex) {
            GlobalLogger.exception(ex, "Command execution error: " + ex);
            String rc = RefCodeMaker.dump("Command execution error: " + command, ex, event.getGuild(), event.getUser());
            sendUserError(s, event, "A system error occurred. Error report reference: " + rc + "\nPlease report this code to the instance owner (" + Main.config.settings.owner + ")");
        }
        Util.logElapsedTime(start);
    }

    private void handleServerIcon(DiscordServer s, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.updateServerIcon = value;
        FF14GameDatabase.save(gs);
        event.reply("Server icon badge function is now " + (value ? "enabled" : "disabled") + ".").setEphemeral(true).queue();
    }

    private void handleSetRaidlistChannel(Server s, SlashCommandInteractionEvent event) {
        OptionMapping r = event.getOption("list-channel");
        long listchannel = 0;
        if (r != null) {
            listchannel = r.getAsChannel().getIdLong();
        }
        OptionMapping postOption = event.getOption("post-channel");
        long postchannel = 0;
        if (postOption != null) {
            postchannel = postOption.getAsChannel().getIdLong();
        }
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.raidlistRoomId = listchannel;
        gs.raidRoomId = postchannel;
        gs.raidlistRoomMessage = 0;
        FF14GameDatabase.save(gs);
        event.reply("Raid list channel successfully changed to: " + (listchannel > 0 ? "<#" + listchannel + ">" : "none") + ".").setEphemeral(true).queue();
    }

    private void handleHildi(SlashCommandInteractionEvent event) {
        event.deferReply().queue(interactionHook -> {
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
                interactionHook.sendMessage("https://ffxiv.consolegameswiki.com/wiki/File:Hildibrand_promo7.png").queue();
            }).start();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        long start = System.currentTimeMillis();
        if (event.getButton() == null || event.getGuild() == null) {
            GlobalLogger.warning("[discord] discarding invalid event button:" + event.getButton() + "/guild:" + event.getGuild());
            return;
        }

        String btn = event.getButton().getId();

        DiscordServer s = api.getServer(event.getGuild());
        DiscordUser u = api.getUser(event.getUser());

        api.getUser(event.getUser()); // hack

        GlobalLogger.fine("[discord] onButtonClick: " + u.getName() + " - " + btn + "(" + event.getGuild().getIdLong() + "/" + event.getUser().getId() + "/" + event.getMessageIdLong() + "/" + btn + ")");

        if (btn == null) {
            return;
        }

        Guild guild = event.getGuild();
        long messageId = event.getMessageIdLong();


        try {
            if (btn.equals("btn-confirm")) {
                handleCreateRaidFromPreview(s, u, event);
                return;
            } else if (btn.equals("btn-unsubscribe")) {
                handleUnsubscribe(s, u, event);
            }

            Raid raid = FF14GameDatabase.getOrCreateGuild(api.getServer(guild)).findRaidByMessageId(messageId);
            if (raid == null) {
                long eph_id = u.getId();
                if (epheremal_messages.containsKey(eph_id)) {
                    raid = epheremal_messages.get(eph_id);
                } else {
                    GlobalLogger.warning("[discord] Invalid interaction: " + messageId + " (interaction: " + eph_id + ") in guild " + guild);
                    sendUserError(s, event, "The operation timed out. Please try whatever you attempted to do from the beginning.");
                    return;
                }
            }

            if (btn.equals("btn-join")) {
                handleJoin(s, u, event, raid.runid, Collections.emptyList(), false, false);
            } else if (btn.equals("btn-backup")) {
                handleJoin(s, u, event, raid.runid, Collections.emptyList(), false, true);
            } else if (btn.equals("btn-maybe")) {
                handleJoin(s, u, event, raid.runid, Collections.emptyList(), true, false);
            } else if (btn.equals("btn-cancel")) {
                handleLeave(s, u, event, raid, true);
            } else if (btn.equals("btn-edit")) {
                handleEdit(s, u, event, raid);
            } else if (btn.equals("btn-open-classes")) {
                handleEditClasses(s, u, event, raid);
            } else if (btn.equals("btn-role-melee")) {
                handleRoleToggle(s, u, event, raid, "MDPS");
            } else if (btn.equals("btn-role-ranged")) {
                handleRoleToggle(s, u, event, raid, "PRDPS");
            } else if (btn.equals("btn-role-caster")) {
                handleRoleToggle(s, u, event, raid, "MRDPS");
            } else if (btn.equals("btn-role-tank")) {
                handleRoleToggle(s, u, event, raid, "TANK");
            } else if (btn.equals("btn-role-healer")) {
                handleRoleToggle(s, u, event, raid, "HEALER");
            } else if (btn.equals("btn-role-pheal")) {
                handleRoleToggle(s, u, event, raid, "PHEAL");
            } else if (btn.equals("btn-role-bheal")) {
                handleRoleToggle(s, u, event, raid, "BHEAL");
            } else if (btn.startsWith("btn-c-")) {
                handleRoleToggle(s, u, event, raid, btn.substring(6));
            } else if (btn.equals("btn-role-1")) {
                handleRoleToggle(s, u, event, raid, "ONE");
            } else if (btn.equals("btn-role-2")) {
                handleRoleToggle(s, u, event, raid, "TWO");
            } else if (btn.equals("btn-role-3")) {
                handleRoleToggle(s, u, event, raid, "THREE");
            } else if (btn.equals("btn-role-4")) {
                handleRoleToggle(s, u, event, raid, "FOUR");
            } else if (btn.equals("btn-role-5")) {
                handleRoleToggle(s, u, event, raid, "FIVE");
            } else {
                sendUserError(s, event, "Unhandled button: " + btn);
            }
        } catch (Exception ex) {
            GlobalLogger.exception(ex, "Button execution error: " + ex);
            String rc = RefCodeMaker.dump("Button execution error: " + btn, ex, event.getGuild(), event.getUser());
            sendUserError(s, event, "A system error occurred. Error report reference: " + rc + "\nPlease report this code to the instance owner (" + Main.config.settings.owner + ")");
        }
        Util.logElapsedTime(start);
    }

    private void handleUnsubscribe(DiscordServer s, DiscordUser u, ButtonInteractionEvent event) {
        FF14GameDatabase.getOrCreateUser(u).notifications = false;
        event.reply("Unsubscribed from all messages on all servers.").queue();
    }

    private void handleCreateRaidFromPreview(DiscordServer s, DiscordUser u, ButtonInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        RaidPreview r = previewCache.get(u.getId());
        if (r == null) {
            sendUserError(s, event, "Could not find a raid preview. This suggests that the preview has expired.");
            return;
        }

        Raid raid = RaidManager.create(gs, r.raid);

        event.reply(api.buildRaidMessageDiscordObject(s, MessageGenerator.generateRaidPost(api, s, raid), false)).queue(interactionHook -> interactionHook.retrieveOriginal().queue(message -> {
            previewCache.remove(u.getId());
            raid.messageid = message.getIdLong();
            raid.channelid = message.getChannel().getIdLong();
            FF14GameDatabase.save(raid);

            if (r.settings.selfJoin) {
                RaidManager.join(raid, u, r.classes, false, false);
                s.recreateRaidMessage(raid);
            }

            createThreadFor(event, message, raid, r.settings.selfJoin);
        }));
    }

    private void createThreadFor(IReplyCallback event, Message message, Raid raid, boolean auto_add) {
        message.createThreadChannel("Raid #" + raid.runid + ": " + raid.name).queue(thread -> {
            raid.threadid = thread.getIdLong();
            if (auto_add) {
                addUserToThread(event.getUser(), raid);
            }
        });
    }

    private void addUserToThread(User user, Raid raid) {
        if (user == null) {
            GlobalLogger.warning("[discord] attempted to add null user to thread");
            return;
        }
        if (raid.threadid != 0) {
            ThreadChannel channel = api.getDiscord().getThreadChannelById(raid.threadid);
            if (channel != null && !channel.isLocked()) {
                if (channel.isArchived()) {
                    channel.getManager().setArchived(false).queue(unused -> {
                        channel.addThreadMember(user).queue();
                    });
                } else {
                    channel.addThreadMember(user).queue();
                }
            } else {
                GlobalLogger.warning("[discord] no thread found: " + raid.threadid);
                raid.threadid = 0;
            }
        }
    }

    private void removeUserFromThread(User user, Raid raid) {
        if (raid.threadid != 0) {
            ThreadChannel channel = api.getDiscord().getThreadChannelById(raid.threadid);
            if (channel != null) {
                channel.removeThreadMember(user).queue();
            } else {
                GlobalLogger.warning("[discord] no thread found: " + raid.threadid);
                raid.threadid = 0;
            }
        }
    }

    private void handleModifyAdd(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        User target = event.getOption("player").getAsUser();
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }
        if (target == null) {
            sendUserError(s, event, "Invalid user specified.");
            return;
        }

        if (raid.hasUser(target.getIdLong())) {
            sendUserError(s, event, "This user has already joined this raid.");
            return;
        }

        String classesStr = "";
        OptionMapping omclassesStr = event.getOption("classes");
        if (omclassesStr != null) {
            classesStr = omclassesStr.getAsString();
        }
        List<FF14Class> classes;
        try {
            classes = FF14GameDatabase.parseClassesFromString(classesStr);
        } catch (IllegalArgumentException e) {
            sendUserError(s, event, "Class \"" + e.getMessage() + "\" is not recognized.");
            return;
        }

        boolean isMaybe = false;
        OptionMapping omMaybe = event.getOption("is-maybe");
        if (omMaybe != null) {
            isMaybe = omMaybe.getAsBoolean();
        }

        boolean isBackup = false;
        OptionMapping omBackup = event.getOption("is-backup");
        if (omBackup != null) {
            isBackup = omBackup.getAsBoolean();
        }

        RaidManager.join(raid, api.getUser(target), classes, isMaybe, isBackup);

        s.recreateRaidMessage(raid);

        event.reply("You have successfully added player <@" + target.getIdLong() + "> to raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();

        addUserToThread(event.getUser(), raid);

    }

    private void handleModifyRemove(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        User target = event.getOption("player").getAsUser();
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }
        if (target == null) {
            sendUserError(s, event, "Invalid user specified.");
            return;
        }

        if (!raid.hasUser(target.getIdLong())) {
            sendUserError(s, event, "This user has not joined this raid.");
            return;
        }

        RaidManager.leave(raid, FF14GameDatabase.getOrCreateUser(api.getUser(target)));

        s.recreateRaidMessage(raid);

        event.reply("You have successfully removed player <@" + target.getIdLong() + "> from raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();

        removeUserFromThread(event.getUser(), raid);
    }


    private void sendUserError(DiscordServer s, IReplyCallback event, String error) {
        event.reply(api.getChatFormatter().getIcon(s, "FF14Error") + " There was a problem with your command:\n" + error).setEphemeral(true).queue();
    }

    private void handleCreateRaid(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event, boolean preview) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);

        String name = event.getOption("raid-name").getAsString();
        String timeStr = event.getOption("start-time").getAsString();
        String desc = "";
        OptionMapping omdescription = event.getOption("description");
        if (omdescription != null) {
            desc = omdescription.getAsString();
        }
        String settings = "";
        OptionMapping omsettings = event.getOption("settings");
        if (omsettings != null) {
            settings = omsettings.getAsString();
        }
        String classesStr = "";
        OptionMapping omclassesStr = event.getOption("classes");
        if (omclassesStr != null) {
            classesStr = omclassesStr.getAsString();
        }
        int hardcap = 99;
        OptionMapping omHardCap = event.getOption("player-cap");
        if (omHardCap != null) {
            hardcap = (int) omHardCap.getAsLong();
        }
        int voting = 0;
        OptionMapping omVoting = event.getOption("voting");
        if (omVoting != null) {
            voting = (int) omVoting.getAsLong();
        }
        int length = 0;
        OptionMapping omLength = event.getOption("length");
        if (omLength != null) {
            length = (int) omLength.getAsLong();
        }

        Instance target = FF14GameDatabase.getInstanceByName(name);

        if (target == null) {
            if (gs.allowUnknownRaids) {
                target = new Instance(null, name, 0, 0, 8, InstanceType.OTHER, null);
            } else {
                sendUserError(s, event, "I could not find a dungeon, trial or raid by the name \"" + name + "\"\n(Unknown raids are disallowed by server settings)");
                return;
            }
        }

        Long time = Util.parseTime(timeStr);
        if (time == null) {
            sendUserError(s, event, "I could not understand \"" + timeStr + "\" for the raid time you specified. Try natural words such as \"tomorrow 18:00\" or exact dates \"15th August 13:00\"");
            return;
        }

        RaidSettings rs;
        try {
            rs = MessageGenerator.parseRaidSettings(settings);
        } catch (IllegalArgumentException e) {
            sendUserError(s, event, "There was an error with the raid settings you specified. The setting \"" + e.getMessage() + "\" is not valid.");
            return;
        }

        List<FF14Class> classes;
        try {
            classes = FF14GameDatabase.parseClassesFromString(classesStr);
        } catch (IllegalArgumentException e) {
            sendUserError(s, event, "Class \"" + e.getMessage() + "\" is not recognized.");
            return;
        }

        Raid raid = preview ? new Raid(0, gs, us, target.lsid, target.name, desc, time, rs.noSprout, rs.noMulti) : RaidManager.create(gs, us, target, desc, time, rs.noSprout, rs.noMulti);
        raid.applyRaidSettings(rs);
        raid.hardCap = hardcap;
        if (voting > 0) {
            raid.isVoting = true;
            raid.votingMaxNum = voting;
        }
        raid.lengthInHrs = length;
        if (timeStr.equalsIgnoreCase("now")) {
            raid.isAutoLocked = true;
            raid.isNotified = true;
        }

        event.reply(api.buildRaidMessageDiscordObject(s, MessageGenerator.generateRaidPost(api, s, raid), preview)).setEphemeral(preview).queue(interactionHook -> interactionHook.retrieveOriginal().queue(message -> {
            if (!preview) {
                raid.messageid = message.getIdLong();
                raid.channelid = message.getChannel().getIdLong();
                FF14GameDatabase.save(raid);

                if (rs.selfJoin) {
                    RaidManager.join(raid, u, classes, false, false);
                    s.recreateRaidMessage(raid);
                }
                createThreadFor(event, message, raid, true);
            } else {
                previewCache.put(us.backendId, new RaidPreview(raid, rs, classes));
            }
        }));
    }

    private void handleJoin(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event, boolean maybe, boolean backup) {
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        List<FF14Class> classes;
        try {
            classes = FF14GameDatabase.parseClassesFromString(optionToClassString(event.getOption("classes")));
        } catch (IllegalArgumentException e) {
            sendUserError(s, event, "Class \"" + e.getMessage() + "\" is not recognized.");
            return;
        }
        handleJoin(s, u, event, raidnr, classes, maybe, backup);
    }

    private String optionToClassString(OptionMapping classes) {
        String str = "";
        if (classes != null) {
            str = classes.getAsString();
        }
        return str;
    }

    private void handleJoin(DiscordServer s, DiscordUser u, IReplyCallback event, int raidnr, List<FF14Class> classes, boolean maybe, boolean backup) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        if (maybe && !gs.allowMaybe) {
            sendUserError(s, event, "The \"/join maybe\" function has been disabled on this server.");
            return;
        }

        if (us.lateLeaveCount >= Main.config.settings.late_leave_cap) {
            backup = false;
            maybe = true;
        }

        if (raid.hasUser(u)) {
            RaidMember rm = raid.getMember(u);
            rm.setJoinState(maybe, backup);
            if (!gs.retainTogglePosition && !rm.isClonedEntry) {
                raid.shoveMemberDown(rm);
            }
            s.recreateRaidMessage(raid);
            addUserToThread(event.getUser(), raid);
            event.reply("You have successfully changed your sign-up for raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
            return;
        }

        int max = 8;
        Instance i = FF14GameDatabase.getInstanceByLodestoneId(raid.lodestoneid);
        if (i != null) {
            max = i.players;
        }

        if (raid.noMulti && raid.getMemberCount() >= max && !maybe && !backup) {
            sendUserError(s, event, "This raid is currently filled, and does not allow more people than one full group.");
            return;
        }
        if (raid.getMemberCount() >= raid.hardCap && !maybe && !backup) {
            sendUserError(s, event, "This raid is currently full. (maximum cap of " + raid.hardCap + " set by raid leader)");
            return;
        }

        if (raid.isLocked) {
            sendUserError(s, event, "This raid is currently locked. You can't join or leave this raid until a raid leader unlocks the raid.");
            return;
        }

        RaidManager.join(raid, u, classes, maybe, backup);

        s.recreateRaidMessage(raid);

        if (us.savedClasses == null || us.savedClasses.isBlank()) {
            openEditMenu(s, u, us, event, raid, api.getChatFormatter().getIcon(s, "FF14OK") + " You have successfully joined raid #" + raid.runid + ": " + raid.name + ".");
        } else {
            event.reply(api.getChatFormatter().getIcon(s, "FF14OK") + " You have successfully joined raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
        }

        addUserToThread(event.getUser(), raid);

    }

    private void handleLeave(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        int raidnr = (int) event.getOption("raid-number").getAsLong();
        Raid raid = FF14GameDatabase.getOrCreateGuild(s).getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }
        OptionMapping stayThreadOm = event.getOption("stay-in-thread");
        boolean stayThread = false;
        if (stayThreadOm != null) {
            stayThread = stayThreadOm.getAsBoolean();
        }
        handleLeave(s, u, event, raid, stayThread);
    }

    private void handleLock(DiscordServer s, SlashCommandInteractionEvent event) {
        int raidnr = (int) event.getOption("raid-number").getAsLong();
        Raid raid = FF14GameDatabase.getOrCreateGuild(s).getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }
        boolean value = event.getOption("lock").getAsBoolean();

        raid.isLocked = value;
        FF14GameDatabase.save(raid);
        s.recreateRaidMessage(raid);
        event.reply("Raid #" + raidnr + " was successfully " + (value ? "locked" : "unlocked") + ".").setEphemeral(true).queue();
    }

    private void handleDelete(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        int raidnr = (int) event.getOption("raid-number").getAsLong();
        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }
        gs.removeRaid(raid);
        for (RaidMember rm : raid.getMembers()) {
            FF14GameDatabase.getDB().delete(rm);
        }
        // hack: so if you have a better idea about this, feel free to but it's gonna be painful
        FF14GameDatabase.executeRaw("DELETE FROM RaidMember WHERE raid = ?1", raid);
        FF14GameDatabase.save(gs);
        s.deleteMessage(raid);
        event.reply("You have successfully deleted raid #" + raid.runid + ": " + raid.name).setEphemeral(true).queue();
    }

    private void handleEdit(DiscordServer s, DiscordUser u, IReplyCallback event, Raid raid) {

        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        if (raid.isLocked) {
            sendUserError(s, event, "This raid is currently locked. You can't join or leave this raid until a raid leader unlocks the raid.");
            return;
        }

        UserSettings user = FF14GameDatabase.getOrCreateUser(u);

        openEditMenu(s, u, user, event, raid);
    }

    private void handleEditClasses(DiscordServer s, DiscordUser u, IReplyCallback event, Raid raid) {

        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        if (raid.isLocked) {
            sendUserError(s, event, "This raid is currently locked. You can't join or leave this raid until a raid leader unlocks the raid.");
            return;
        }

        UserSettings user = FF14GameDatabase.getOrCreateUser(u);

        openEditMenuCl(s, u, user, event, raid);
    }

    private void openEditMenu(DiscordServer s, DiscordUser u, UserSettings user, IReplyCallback event, Raid raid) {
        openEditMenu(s, u, user, event, raid, null);
    }

    private void openEditMenu(DiscordServer s, DiscordUser u, UserSettings user, IReplyCallback event, Raid raid, String override_message) {
        ArrayList<ActionRow> ars = new ArrayList<>();

        Guild g = event.getGuild();

        RaidMember rm = raid.getMember(u);

        ars.add(ActionRow.of(
                Button.secondary("btn-role-melee", api.getEmojiCls(g, "MDPS")),
                Button.secondary("btn-role-ranged", api.getEmojiCls(g, "PRDPS")),
                Button.secondary("btn-role-caster", api.getEmojiCls(g, "MRDPS")),
                Button.secondary("btn-role-tank", api.getEmojiCls(g, "TANK")),
                Button.secondary("btn-role-healer", api.getEmojiCls(g, "HEALER"))
        ));

        ars.add(ActionRow.of(
                Button.secondary("btn-role-pheal", api.getEmojiCls(g, "PHEAL")),
                Button.secondary("btn-role-bheal", api.getEmojiCls(g, "BHEAL"))
        ));

        if (raid.isVoting) {
            ArrayList<Button> btns = new ArrayList<>();
            for (int i = 1; i <= 5 && i <= raid.votingMaxNum; i++) {
                btns.add(Button.secondary("btn-role-" + i, i + "️"));
            }
            if (!btns.isEmpty()) {
                ars.add(ActionRow.of(btns));
            }
        }
        ars.add(ActionRow.of(
                Button.secondary("btn-open-classes", "Select jobs")
                        .withEmoji(api.getEmojiCls(g, "FF14Edit"))
        ));

        event.reply((override_message != null ? override_message : api.getChatFormatter().getIcon(s, "FF14Edit") + " Settings for Raid #" + raid.runid + ": " + raid.name) + "\nUse the role buttons to mark the role(s) you want to play." + (raid.isVoting ? "\nUse the number buttons to vote." : "")).addComponents(ars).setEphemeral(true).queue(interactionHook -> {
            long id = user.backendId;
            GlobalLogger.fine("Epheremal message id for raid editor: " + id);
            epheremal_messages.put(id, raid);
        });
    }

    private void openEditMenuCl(DiscordServer s, DiscordUser u, UserSettings user, IReplyCallback event, Raid raid) {
        ArrayList<ActionRow> ars = new ArrayList<>();

        Guild g = event.getGuild();

        ars.add(getActionRowForJobs(g, "TANK"));
        ars.add(getActionRowForJobs(g, "HEALER"));
        ars.add(getActionRowForJobs(g, "MDPS"));
        ars.add(getActionRowForJobs(g, "PRDPS"));
        ars.add(getActionRowForJobs(g, "MRDPS"));

        event.reply(api.getChatFormatter().getIcon(s, "FF14Edit") + " Job Selection for Raid #" + raid.runid + ": " + raid.name).addComponents(ars).setEphemeral(true).queue(interactionHook -> {
            long id = user.backendId;
            GlobalLogger.fine("Epheremal message id for raid editor: " + id);
            epheremal_messages.put(id, raid);
        });
    }

    private ActionRow getActionRowForJobs(Guild g, String classn) {
        ArrayList<Button> btns = new ArrayList<>();

        for (FF14Class cl : FF14GameDatabase.getClassByCategory(classn)) {
            if (cl.showbutton) {
                btns.add(Button.secondary("btn-c-" + cl.shortname, api.getEmojiCls(g, cl.shortname)));
            }
        }

        return ActionRow.of(btns);
    }

    private void handleLeave(DiscordServer s, DiscordUser u, IReplyCallback event, Raid raid, boolean stayThread) {
        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        if (raid.isLocked) {
            sendUserError(s, event, "This raid is currently locked. You can't join or leave this raid until a raid leader unlocks the raid.");
            return;
        }

        UserSettings user = FF14GameDatabase.getOrCreateUser(u);

        if (!raid.getMember(u).isBackup && !raid.getMember(u).isMaybe && System.currentTimeMillis() + Util.hoursToMs(24) > raid.time) {
            user.lateLeaveCount++;
        }

        RaidManager.leave(raid, user);

        s.recreateRaidMessage(raid);
        event.reply("You have successfully left raid #" + raid.runid + ": " + raid.name).setEphemeral(true).queue();

        if (!stayThread) {
            removeUserFromThread(event.getUser(), raid);
        }
    }

    private void handleSetModeratorRole(DiscordServer s, SlashCommandInteractionEvent event) {
        OptionMapping r = event.getOption("role");
        long role = 0;
        if (r != null) {
            role = r.getAsRole().getIdLong();
        }
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.raidLeaderRoleId = role;
        FF14GameDatabase.save(gs);
        s.updateCommandPermissions();
        event.reply("Raid Leader role successfully changed to: " + (role > 0 ? "<@&" + role + ">" : "none") + ".").setEphemeral(true).queue();
    }

    private void handleSetAdminRole(DiscordServer s, SlashCommandInteractionEvent event) {
        OptionMapping r = event.getOption("role");
        long role = 0;
        if (r != null) {
            role = r.getAsRole().getIdLong();
        }
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.administratorRoleId = role;
        FF14GameDatabase.save(gs);
        s.updateCommandPermissions();
        event.reply("Administrator role successfully changed to: " + (role > 0 ? "<@&" + role + ">" : "none") + ".").setEphemeral(true).queue();
    }

    private void handleSetCreateRole(DiscordServer s, SlashCommandInteractionEvent event) {
        OptionMapping r = event.getOption("role");
        long role = 0;
        if (r != null) {
            role = r.getAsRole().getIdLong();
        }
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.createRoleId = role;
        FF14GameDatabase.save(gs);
        s.updateCommandPermissions();
        event.reply("Creator role successfully changed to: " + (role > 0 ? "<@&" + role + ">" : "none") + ".").setEphemeral(true).queue();
    }

    private void handleSetNotifyRole(DiscordServer s, SlashCommandInteractionEvent event) {
        OptionMapping r = event.getOption("role");
        long role = 0;
        if (r != null) {
            role = r.getAsRole().getIdLong();
        }
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.notifyRoleId = role;
        FF14GameDatabase.save(gs);
        s.updateCommandPermissions();
        event.reply("Player notification role successfully changed to: " + (role > 0 ? "<@&" + role + ">" : "none") + ".").setEphemeral(true).queue();
    }

    private void handleSetMaybe(DiscordServer s, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.allowMaybe = value;
        FF14GameDatabase.save(gs);
        event.reply("\"/joinmaybe\" function is now " + (value ? "enabled" : "disabled") + ".").setEphemeral(true).queue();
    }

    private void handleSetUnknown(DiscordServer s, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("allowed").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.allowUnknownRaids = value;
        FF14GameDatabase.save(gs);
        event.reply("Unknown raids are now " + (value ? "enabled" : "disabled") + ".").setEphemeral(true).queue();
    }

    private void handleSetTogglePosition(DiscordServer s, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("retain-position").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.retainTogglePosition = value;
        FF14GameDatabase.save(gs);
        event.reply("Join toggle position is " + (value ? "now" : "no longer") + " retained.").setEphemeral(true).queue();
    }

    private void handleSetNotifyTime(DiscordServer s, SlashCommandInteractionEvent event) {
        int value = (int) event.getOption("minutes").getAsLong();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.notifyMinutes = value;
        FF14GameDatabase.save(gs);
        event.reply("Raid notifications are now " + (value > 0 ? "set to " + value + " minute(s)" : "off") + ".").setEphemeral(true).queue();
    }

    private void handleSetAutoLockTime(DiscordServer s, SlashCommandInteractionEvent event) {
        int value = (int) event.getOption("minutes").getAsLong();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        gs.autoLockMinutes = value;
        FF14GameDatabase.save(gs);
        event.reply("Raids will now be " + (value >= 0 ? "locked " + value + " minute(s) before start" : " not automatically locked") + ".").setEphemeral(true).queue();
    }

    private void handleSetCamera(DiscordUser u, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);
        us.camera = value;
        FF14GameDatabase.save(us);
        event.reply("You are " + (value ? "now" : "no longer") + " showing as a player recording video/audio.").setEphemeral(true).queue();
    }

    private void handleSetPingForRaids(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        Guild g = s.getDiscordGuild();

        Role r = g.getRoleById(gs.notifyRoleId);

        if (gs.notifyRoleId == 0 || r == null) {
            sendUserError(s, event, "This feature is not configured.");
            return;
        }

        if (value) {
            g.addRoleToMember(UserSnowflake.fromId(u.getId()), r).queue();
        } else {
            g.removeRoleFromMember(UserSnowflake.fromId(u.getId()), r).queue();
        }

        event.reply("You are " + (value ? "now" : "no longer") + " getting pinged for raids.").setEphemeral(true).queue();
    }

    private void handleSetIconForTomorrowOnly(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        Guild g = s.getDiscordGuild();

        Role r = g.getRoleById(gs.notifyRoleId);

        gs.iconOnlyForTomorrowRaids = value;

        event.reply("Setting updated to " + value + ".").setEphemeral(true).queue();
    }

    private void handleSetNotifications(DiscordUser u, SlashCommandInteractionEvent event) {
        boolean value = event.getOption("enable").getAsBoolean();
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);
        us.notifications = value;
        FF14GameDatabase.save(us);
        event.reply("You are " + (value ? "now" : "no longer") + " receiving raid notifications from this server.").setEphemeral(true).queue();
    }

    private void handleSetCharacterName(DiscordUser u, SlashCommandInteractionEvent event) {
        String value = event.getOption("name").getAsString();
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);
        us.nickname = value;
        FF14GameDatabase.save(us);
        event.reply("You will now show up as \"" + (value) + "\" in raid member lists.").setEphemeral(true).queue();
    }

    private void handleRoleToggle(DiscordServer s, DiscordUser u, ButtonInteractionEvent event, Raid raid, String classname) {
        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        if (raid.isLocked) {
            sendUserError(s, event, "This raid is currently locked. You can't join or leave this raid until a raid leader unlocks the raid.");
            return;
        }

        FF14Class clazz = FF14GameDatabase.getClass(classname);
        if (clazz == null) {
            sendUserError(s, event, "The following class/role is not recognized: " + classname);
            return;
        }

        RaidMember rm = raid.getMember(u);
        if (rm.classes.contains(clazz)) {
            rm.classes.remove(clazz);
        } else {
            rm.classes.add(clazz);
        }

        FF14GameDatabase.save(rm);

        event.deferEdit().queue(interactionHook -> {
            s.recreateRaidMessage(raid);
        });

    }

    private void handleSetClasses(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);

        int raidnr = (int) event.getOption("raid-number").getAsLong();
        Raid raid = FF14GameDatabase.getOrCreateGuild(s).getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        String classesStr = "";
        OptionMapping omclassesStr = event.getOption("classes");
        if (omclassesStr != null) {
            classesStr = omclassesStr.getAsString();
        }

        ArrayList<FF14Class> classes = new ArrayList<>();

        for (String classStr : Util.discordArgParse(classesStr)) {
            FF14Class clazz = FF14GameDatabase.getClass(classStr);
            if (clazz == null) {
                sendUserError(s, event, "The following class/role is not recognized: " + classStr);
                return;
            }
            classes.add(clazz);
        }

        RaidMember rm = raid.getMember(u);
        rm.classes = classes;
        FF14GameDatabase.save(rm);
        s.recreateRaidMessage(raid);
        event.reply("Successfully changed classes for raid #" + raidnr + ".").setEphemeral(true).queue();
    }

    private void handleSaveClasses(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);

        int raidnr = (int) event.getOption("raid-number").getAsLong();
        Raid raid = FF14GameDatabase.getOrCreateGuild(s).getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        if (!raid.hasUser(u)) {
            sendUserError(s, event, "You have not joined raid #" + raid.runid + ": " + raid.name + ".");
            return;
        }

        List<FF14Class> classes = raid.getMember(u).classes;

        StringBuilder clsStr = new StringBuilder();
        for (FF14Class c : classes) {
            clsStr.append(c.shortname);
            clsStr.append(" ");
        }

        us.savedClasses = clsStr.toString();
        FF14GameDatabase.save(us);

        event.reply("Successfully set your preferred classes to \"" + us.savedClasses + "\"").setEphemeral(true).queue();
    }

    private void handleModifyEditName(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        String name = event.getOption("name").getAsString();
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        raid.name = name;
        Instance i = FF14GameDatabase.getInstanceByName(name);
        if (i != null) {
            raid.lodestoneid = i.lsid;
            raid.name = i.name;
        } else {
            raid.lodestoneid = null;
        }
        FF14GameDatabase.save(raid);
        s.recreateRaidMessage(raid);

        event.reply("You have successfully edited raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
    }

    private void handleModifyEditDesc(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        String desc = event.getOption("new-description").getAsString();
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        raid.setDescriptionParsed(desc);
        FF14GameDatabase.save(raid);

        s.recreateRaidMessage(raid);

        event.reply("You have successfully edited raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
    }

    private void handleModifyEditTime(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        String timestr = event.getOption("start-time").getAsString();
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        Long time = Util.parseTime(timestr);
        if (time == null) {
            sendUserError(s, event, "I could not understand the raid time you specified. Try natural words such as \"tomorrow 18:00\" or exact dates \"15th August 13:00\"");
            return;
        }

        raid.time = time;

        FF14GameDatabase.save(raid);

        s.recreateRaidMessage(raid);

        event.reply("You have successfully edited raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
    }

    private void handleModifyEditOptions(DiscordServer s, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);

        String options = "";
        OptionMapping optionom = event.getOption("settings");
        if (optionom != null) {
            options = optionom.getAsString();
        }
        int raidnr = (int) event.getOption("raid-number").getAsLong();

        Raid raid = gs.getRaid(raidnr);
        if (raid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        RaidSettings rs;
        try {
            rs = MessageGenerator.parseRaidSettings(options);
        } catch (IllegalArgumentException e) {
            sendUserError(s, event, "There was an error with the raid settings you specified. The setting \"" + e.getMessage() + "\" is not valid.");
            return;
        }

        raid.applyRaidSettings(rs);
        FF14GameDatabase.save(raid);

        s.recreateRaidMessage(raid);

        event.reply("You have successfully edited raid #" + raid.runid + ": " + raid.name + ".").setEphemeral(true).queue();
    }

    private void handleClone(DiscordServer s, DiscordUser u, SlashCommandInteractionEvent event) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
        UserSettings us = FF14GameDatabase.getOrCreateUser(u);

        int raidnr = (int) event.getOption("raid-number").getAsLong();
        boolean clonePlayers = event.getOption("clone-players").getAsBoolean();

        Raid originalRaid = gs.getRaid(raidnr);
        if (originalRaid == null) {
            sendUserError(s, event, "There is no raid #" + raidnr + ".");
            return;
        }

        String desc = originalRaid.description;
        OptionMapping omDescription = event.getOption("new-description");
        if (omDescription != null) {
            desc = omDescription.getAsString();
        }

        Long time = Util.parseTime(event.getOption("new-start-time").getAsString());
        if (time == null) {
            sendUserError(s, event, "I could not understand the raid time you specified. Try natural words such as \"tomorrow 18:00\" or exact dates \"15th August 13:00\"");
            return;
        }

        Instance target = FF14GameDatabase.getInstanceByLodestoneId(originalRaid.lodestoneid);
        if (target == null) {
            target = new Instance(null, originalRaid.name, 0, 0, 8, InstanceType.OTHER, null);
        }

        Raid raid = RaidManager.create(gs, us, target, desc, time, originalRaid.noSprout, originalRaid.noMulti);
        raid.applyRaidSettings(originalRaid);

        if (clonePlayers) {
            for (RaidMember r : originalRaid.getMembers()) {
                if (!r.isBackup && !r.isMaybe) {
                    RaidManager.join(raid, r.user, r.classes, true, false, true);

                }
            }
        }

        event.reply(api.buildRaidMessageDiscordObject(s, MessageGenerator.generateRaidPost(api, s, raid), false)).queue(interactionHook -> interactionHook.retrieveOriginal().queue(message -> {
            raid.messageid = message.getIdLong();
            raid.channelid = message.getChannel().getIdLong();
            FF14GameDatabase.save(raid);

            createThreadFor(event, message, raid, false);

            if (clonePlayers) {
                for (RaidMember r : originalRaid.getMembers()) {
                    addUserToThread(api.getDiscord().getUserById(r.user.backendId), raid);
                }
            }
        }));


    }

    private void handleReinitialize(DiscordServer s, SlashCommandInteractionEvent event) {

        api.initializeGuild(event.getGuild());
        try {
            s.setServerIcon(s.getGuildSettings(), s.getGuildSettings().getOpenRaids().size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        event.reply("Action successful. Some things might need a minute or two until they get refreshed by Discord.").setEphemeral(true).queue();
    }

    private void handleVersion(SlashCommandInteractionEvent event) {
        event.reply(Util.getInfoString(api)).setEphemeral(true).queue();
    }

}
