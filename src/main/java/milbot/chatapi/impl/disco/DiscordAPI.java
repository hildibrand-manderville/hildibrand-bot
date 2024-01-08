package milbot.chatapi.impl.disco;

import eu.haruka.jpsfw.logging.GlobalLogger;
import milbot.Config;
import milbot.Main;
import milbot.chatapi.ChatAPI;
import milbot.chatapi.ChatFormatter;
import milbot.chatapi.CommandHelper;
import milbot.chatapi.User;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.RaidListMessage;
import milbot.objects.RaidMessage;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;
import milbot.objects.db.UserSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordAPI implements ChatAPI<DiscordServer>, ChatFormatter<DiscordServer> {

    private JDA discord;

    // hack: so apparently because users interacting with the api do not get cached I have to DOUBLE CACHE every dang user or I would need to dispatch an async request...
    private HashMap<Long, net.dv8tion.jda.api.entities.User> userInteractionDoubleCache;
    private HashMap<Long, DiscordUser> userCache;
    private HashMap<Long, DiscordServer> serverCache;

    @Override
    public DiscordUser getUserById(long id) {

        DiscordUser u;
        if (userCache.containsKey(id)) {
            u = userCache.get(id);
        } else {
            net.dv8tion.jda.api.entities.User dcu = discord.getUserById(id);

            // hack
            if (dcu == null) {
                if (userInteractionDoubleCache.containsKey(id)) {
                    dcu = userInteractionDoubleCache.get(id);
                } else {
                    GlobalLogger.severe("[discord] Attempt to access user by id " + id + " which is neither cached in JDA or locally");
                    return null;
                }
            }
            u = new DiscordUser(this, dcu);
            userCache.put(id, u);
        }

        return u;
    }

    public DiscordUser getUser(net.dv8tion.jda.api.entities.User user) {
        if (user == null) {
            throw new NullPointerException("user passed to DiscordAPI.getUser was null");
        }

        // hack
        if (!userInteractionDoubleCache.containsKey(user.getIdLong())) {
            GlobalLogger.fine("[discord] Caching: " + user);
            userInteractionDoubleCache.put(user.getIdLong(), user);
        }

        return new DiscordUser(this, user);
    }

    public DiscordServer getServer(Guild g) {
        if (g == null) {
            throw new NullPointerException("guild passed to DiscordAPI.getServer was null");
        }
        return getServerById(g.getIdLong());
    }

    @Override
    public DiscordServer getServerById(long id) {
        if (!serverCache.containsKey(id)) {
            serverCache.put(id, new DiscordServer(this, discord.getGuildById(id)));
        }
        return serverCache.get(id);
    }

    @Override
    public void start(Config config, List<String> argl) throws LoginException, InterruptedException {
        userInteractionDoubleCache = new HashMap<>();
        userCache = new HashMap<>();
        serverCache = new HashMap<>();
        GlobalLogger.info("[discord] Logging into discord...");
        discord = JDABuilder.createLight(config.discord.token, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
                .enableCache(CacheFlag.EMOJI)
                .addEventListeners(new DiscordEvents(this))
                //.setActivity(Activity.of(Activity.ActivityType.valueOf(config.discord.status_type), config.discord.status))
                .build();
        discord.awaitReady();
        GlobalLogger.info("Logged in!");

        for (Guild g : discord.getGuilds()) {
            DiscordServer s = getServer(g);
            GuildSettings gs = FF14GameDatabase.getOrCreateGuild(s);
            if (!gs.initialized || argl.contains("--reinitialize")) {
                initializeGuild(g);
                gs.initialized = true;
            }
        }

        // cache hack
        for (UserSettings us : FF14GameDatabase.loadAllData(UserSettings.class)) {
            try {
                var discorduser = discord.retrieveUserById(us.backendId).complete();
                if (discorduser != null) {
                    getUser(discorduser);
                }
            } catch (Exception ignored) {
                GlobalLogger.warning("[discord] cache retrieval of " + us.backendId + " failed");
            }
        }
    }

    @Override
    public ChatFormatter<DiscordServer> getChatFormatter() {
        return this;
    }

    @Override
    public void sendDMTo(long backendId, String message, String raidUrl) {
        ArrayList<ActionRow> ars = new ArrayList<>();
        if (raidUrl != null) {
            ars.add(ActionRow.of(
                    Button.link(raidUrl, "View raid/server")
            ));
        }
        ars.add(ActionRow.of(
                Button.secondary("btn-unsubscribe", "Unsubscribe from all messages")
        ));
        MessageCreateBuilder mb = new MessageCreateBuilder().setContent(message).setComponents(ars);
        discord.openPrivateChannelById(backendId).queue(privateChannel -> privateChannel.sendMessage(mb.build()).queue(message1 -> {
        }, throwable -> GlobalLogger.exception(throwable, "Failed to contact " + backendId)));
    }

    @Override
    public String buildURLToRaidPost(long serverId, long channelId, long messageId) {
        return "https://discord.com/channels/" + serverId + "/" + channelId + "/" + (messageId > 0 ? messageId : "");
    }

    @Override
    public void checkThreadState(Raid r, long tid) {
        ThreadChannel tc = discord.getThreadChannelById(tid);
        if (tc != null) {
            if (tc.isArchived() && !tc.isLocked()) {
                tc.getManager().setArchived(false);
            }
        } else {
            GlobalLogger.warning("[discord] checkThreadState: no thread found with id: " + tid);
            r.threadid = 0;
        }
    }

    void initializeGuild(Guild g) {
        GlobalLogger.info("[discord] Initializing guild " + g.getIdLong() + " / " + g.getName() + " (owned by " + g.getOwnerIdLong() + ")");
        DiscordServer s = getServer(g);
        g.retrieveEmojis().queue();
        CommandHelper.registerAllCommands(s);
        s.updateCommandPermissions();
        File cache_image = new File(Main.CACHE_DIR, String.valueOf(s.getId()));
        if (cache_image.exists()) {
            try {
                Files.delete(cache_image.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getIcon(DiscordServer server, String emoji) {
        if (emoji.equals("")) {
            return "";
        }
        for (RichCustomEmoji e : server.getDiscordGuild().getEmojiCache()) {
            if (e.getName().equals(emoji)) {
                return e.getAsMention();
            }
        }
        GlobalLogger.warning("[discord] Emoji not found on " + server.getId() + ": " + emoji);
        return ":" + emoji + ":";
    }

    @Override
    public String getIcon(ChatAPI<? extends DiscordServer> api, long serverId, String emoji) {
        return getIcon(api.getServerById(serverId), emoji);
    }

    public String getUser(DiscordServer server, User user) {
        return String.format("<@%d>", user.getId());
    }

    @Override
    public String getInItalic(String str) {
        return String.format("_%s_", str);
    }

    @Override
    public String getInBold(String str) {
        return String.format("**%s**", str);
    }

    @Override
    public String getUnderlined(String str) {
        return String.format("__%s__", str);
    }

    @Override
    public String getTimeString(long time) {
        return String.format("<t:%d:F>", time);
    }

    @Override
    public String getGroup(long groupId) {
        return String.format("<@&%d>", groupId);
    }

    @Override
    public String getURL(String url) {
        return String.format("<%s>", url);
    }

    @Override
    public String getRelativeTimeString(long time) {
        return String.format("<t:%d:R>", time);
    }

    public RichCustomEmoji getEmoteCls(Guild g, String emoji) {
        for (RichCustomEmoji e : g.getEmojiCache()) {
            if (e.getName().equals(emoji)) {
                return e;
            }
        }
        GlobalLogger.warning("Emoji not found on " + g.getIdLong() + ": " + emoji);
        return null;
    }

    public Emoji getEmojiCls(Guild g, String emoji) {
        RichCustomEmoji e = getEmoteCls(g, emoji);
        return e != null ? Emoji.fromCustom(e) : Emoji.fromUnicode("U+2753");
    }

    public MessageCreateData buildRaidMessageDiscordObject(DiscordServer server, RaidMessage rm, boolean preview) {
        Guild g = server.getDiscordGuild();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(server);
        Raid raid = rm.raid;

        EmbedBuilder ebGroupList = new EmbedBuilder()
                .setTitle("Current Setup")
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .setFooter("Last updated");
        ebGroupList.addField("Currently", rm.playerString, false);
        for (RaidMessage.Party p : rm.groups) {
            ebGroupList.addField(p.title, p.names, true);
            ebGroupList.addField("Classes", p.classes, true);
            if (raid.isVoting) {
                ebGroupList.addField("Voting", p.votes, true);
            } else {
                ebGroupList.addField("\u200B", "\u200B", true);
            }
        }

        for (Map.Entry<String, String> am : rm.additionalMessages.entrySet()) {
            ebGroupList.addField(am.getKey(), am.getValue(), false);
        }

        MessageCreateBuilder mb = new MessageCreateBuilder()
                .setContent(rm.header)
                .setEmbeds(new EmbedBuilder()
                                .setTitle("Raid Information")
                                .setDescription(rm.message)
                                .setTimestamp(Instant.ofEpochMilli(raid.created))
                                .setFooter("Created")
                                .setImage(rm.instance.imageUrl)
                                .setThumbnail(rm.instance.type.getIcon())
                                .setColor(Color.RED)
                                .build(),
                        ebGroupList.build()
                );

        ArrayList<ActionRow> ars = new ArrayList<>();
        if (preview) {
            ars.add(ActionRow.of(
                    Button.primary("btn-confirm", "Create this raid!")
                            .withEmoji(getEmojiCls(g, "FF14OK"))
            ));
        }
        ars.add(ActionRow.of(
                Button.primary("btn-join", "Participate in this raid!")
                        .withEmoji(getEmojiCls(g, "PlayerMember"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-backup", "... as backup")
                        .withEmoji(getEmojiCls(g, "PlayerHire"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-maybe", "... as uncertain")
                        .withEmoji(getEmojiCls(g, "FF14Question"))
                        .withDisabled(!gs.allowMaybe || raid.isLocked || preview),
                Button.secondary("btn-edit", "Edit sign-up")
                        .withEmoji(getEmojiCls(g, "FF14Edit"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-cancel", "Rescind sign-up")
                        .withEmoji(getEmojiCls(g, "FF14Error"))
                        .withDisabled(raid.isLocked || preview)
        ));
        ars.add(ActionRow.of(
                Button.link(Main.config.settings.help_url, "Help")
                        .withEmoji(getEmojiCls(g, "PlayerSprout"))
        ));
        return mb.setComponents(ars).build();
    }

    public MessageEditData editRaidMessageDiscordObject(DiscordServer server, RaidMessage rm, boolean preview) {
        Guild g = server.getDiscordGuild();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(server);
        Raid raid = rm.raid;

        EmbedBuilder ebGroupList = new EmbedBuilder()
                .setTitle("Current Setup")
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .setFooter("Last updated");
        ebGroupList.addField("Currently", rm.playerString, false);
        for (RaidMessage.Party p : rm.groups) {
            ebGroupList.addField(p.title, p.names, true);
            ebGroupList.addField("Classes", p.classes, true);
            if (raid.isVoting) {
                ebGroupList.addField("Voting", p.votes, true);
            } else {
                ebGroupList.addField("\u200B", "\u200B", true);
            }
        }

        for (Map.Entry<String, String> am : rm.additionalMessages.entrySet()) {
            ebGroupList.addField(am.getKey(), am.getValue(), false);
        }

        MessageEditBuilder mb = new MessageEditBuilder()
                .setContent(rm.header)
                .setEmbeds(new EmbedBuilder()
                                .setTitle("Raid Information")
                                .setDescription(rm.message)
                                .setTimestamp(Instant.ofEpochMilli(raid.created))
                                .setFooter("Created")
                                .setImage(rm.instance.imageUrl)
                                .setThumbnail(rm.instance.type.getIcon())
                                .setColor(Color.RED)
                                .build(),
                        ebGroupList.build()
                );

        ArrayList<ActionRow> ars = new ArrayList<>();
        if (preview) {
            ars.add(ActionRow.of(
                    Button.primary("btn-confirm", "Create this raid!")
                            .withEmoji(getEmojiCls(g, "FF14OK"))
            ));
        }
        ars.add(ActionRow.of(
                Button.primary("btn-join", "Participate in this raid!")
                        .withEmoji(getEmojiCls(g, "PlayerMember"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-backup", "... as backup")
                        .withEmoji(getEmojiCls(g, "PlayerHire"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-maybe", "... as uncertain")
                        .withEmoji(getEmojiCls(g, "FF14Question"))
                        .withDisabled(!gs.allowMaybe || raid.isLocked || preview),
                Button.secondary("btn-edit", "Edit sign-up")
                        .withEmoji(getEmojiCls(g, "FF14Edit"))
                        .withDisabled(raid.isLocked || preview),
                Button.secondary("btn-cancel", "Rescind sign-up")
                        .withEmoji(getEmojiCls(g, "FF14Error"))
                        .withDisabled(raid.isLocked || preview)
        ));
        ars.add(ActionRow.of(
                Button.link(Main.config.settings.help_url, "Help")
                        .withEmoji(getEmojiCls(g, "PlayerSprout"))
        ));
        return mb.setComponents(ars).build();
    }

    public MessageCreateData buildRaidListMessageDiscordObject(DiscordServer server, RaidListMessage rm) {
        Guild g = server.getDiscordGuild();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(server);

        ArrayList<Button> arraids = new ArrayList<>();
        for (int i = 0; i < 5 && i < rm.raids.size(); i++) {
            Raid r = rm.raids.get(i);
            arraids.add(Button.link(buildURLToRaidPost(gs.backendId, r.channelid, r.messageid), "Jump to raid #" + r.runid)
                    .withDisabled(r.messageid == 0)
                    .withEmoji(getEmojiCls(g, "PlayerMember"))
            );
        }

        ArrayList<ActionRow> arr = new ArrayList<>();
        arr.add(ActionRow.of(
                Button.link(buildURLToRaidPost(gs.backendId, gs.raidRoomId, 0), "Jump to the raiding channel")
                        .withDisabled(gs.raidRoomId == 0)
                        .withEmoji(getEmojiCls(g, "PlayerMember")),
                Button.link(Main.config.settings.help_url, "Help")
                        .withEmoji(getEmojiCls(g, "PlayerSprout"))
        ));
        if (!arraids.isEmpty()) {
            arr.add(ActionRow.of(arraids));
        }

        MessageCreateBuilder mb = new MessageCreateBuilder().setContent(" - Currently open raid recruitments -")
                .setEmbeds(new EmbedBuilder()
                        .setTitle(rm.title)
                        .setDescription(rm.description)
                        .setTimestamp(Instant.now())
                        .setFooter(rm.updatingInfo)
                        .setColor(Color.GREEN)
                        .addField("Name", rm.names.toString(), true)
                        .addField("Starting (LT)", rm.times.toString(), true)
                        .addField("Players", rm.playercounts.toString(), true)
                        .build()
                )
                .setComponents(arr);
        return mb.build();
    }

    public MessageEditData editRaidListMessageDiscordObject(DiscordServer server, RaidListMessage rm) {
        Guild g = server.getDiscordGuild();
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(server);

        ArrayList<Button> arraids = new ArrayList<>();
        for (int i = 0; i < 5 && i < rm.raids.size(); i++) {
            Raid r = rm.raids.get(i);
            arraids.add(Button.link(buildURLToRaidPost(gs.backendId, r.channelid, r.messageid), "Jump to raid #" + r.runid)
                    .withDisabled(r.messageid == 0)
                    .withEmoji(getEmojiCls(g, "PlayerMember"))
            );
        }

        ArrayList<ActionRow> arr = new ArrayList<>();
        arr.add(ActionRow.of(
                Button.link(buildURLToRaidPost(gs.backendId, gs.raidRoomId, 0), "Jump to the raiding channel")
                        .withDisabled(gs.raidRoomId == 0)
                        .withEmoji(getEmojiCls(g, "PlayerMember")),
                Button.link(Main.config.settings.help_url, "Help")
                        .withEmoji(getEmojiCls(g, "PlayerSprout"))
        ));
        if (!arraids.isEmpty()) {
            arr.add(ActionRow.of(arraids));
        }

        MessageEditBuilder mb = new MessageEditBuilder().setContent(" - Currently open raid recruitments -")
                .setEmbeds(new EmbedBuilder()
                        .setTitle(rm.title)
                        .setDescription(rm.description)
                        .setTimestamp(Instant.now())
                        .setFooter(rm.updatingInfo)
                        .setColor(Color.GREEN)
                        .addField("Name", rm.names.toString(), true)
                        .addField("Starting (LT)", rm.times.toString(), true)
                        .addField("Players", rm.playercounts.toString(), true)
                        .build()
                )
                .setComponents(arr);
        return mb.build();
    }

    public JDA getDiscord() {
        return discord;
    }
}
