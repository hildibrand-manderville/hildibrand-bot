package milbot.raid;

import milbot.Main;
import milbot.chatapi.ChatAPI;
import milbot.chatapi.ChatFormatter;
import milbot.chatapi.Server;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.*;
import milbot.objects.db.*;
import milbot.util.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageGenerator {

    public static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    public static RaidSettings parseRaidSettings(String settings) {
        RaidSettings rs = new RaidSettings();

        for (String s : Util.discordArgParse(settings)) {
            if (s.equalsIgnoreCase("no-multi-groups")) {
                rs.noMulti = true;
            } else if (s.equalsIgnoreCase("no-first-timers")) {
                rs.noSprout = true;
            } else if (s.equalsIgnoreCase("no-self-join")) {
                rs.selfJoin = false;
            } else if (s.equalsIgnoreCase("mil")) {
                rs.mil = true;
            } else if (s.equalsIgnoreCase("no-echo")) {
                rs.noecho = true;
            } else if (s.equalsIgnoreCase("unsync")) {
                rs.unsync = true;
            } else if (s.equalsIgnoreCase("no-notifications")) {
                rs.noNotify = true;
            } else {
                throw new IllegalArgumentException(s);
            }
        }

        return rs;
    }

    public static <ServerType extends Server> RaidMessage generateRaidPost(ChatAPI<ServerType> api, ServerType server, Raid raid) {
        GuildSettings gs = FF14GameDatabase.getOrCreateGuild(server);
        ChatFormatter<ServerType> cf = api.getChatFormatter();

        RaidMessage rm = new RaidMessage(raid);

        Instance i = FF14GameDatabase.getInstanceByLodestoneId(raid.lodestoneid);
        if (i == null) {
            i = new Instance(null, raid.name, 0, 0, 8, InstanceType.OTHER, null);
        }
        rm.instance = i;

        ArrayList<ArrayList<RaidMember>> groups = new ArrayList<>();
        ArrayList<RaidMember> currentGroup = new ArrayList<>();
        int total = 0;
        int maybecnt = 0;
        for (RaidMember r : raid.getMembers()) {
            if (r.isMaybe || r.isBackup) {
                continue;
            }
            currentGroup.add(r);
            total++;
            if (currentGroup.size() >= i.players) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
        }
        for (RaidMember r : raid.getMembers()) {
            if (!r.isMaybe && !r.isBackup) {
                continue;
            }
            if (r.isMaybe) {
                maybecnt++;
            }
            currentGroup.add(r);
            total++;
            if (currentGroup.size() >= i.players) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
        }
        if (currentGroup.size() > 0) {
            groups.add(currentGroup);
        }

        rm.playerString = String.format("%d/%d %s players %s", total - maybecnt, raid.hardCap < 99 ? raid.hardCap : i.players, maybecnt > 0 ? "(+" + maybecnt + ")" : "", groups.size() >= 2 ? "in " + groups.size() + " groups" : total == i.players && !raid.noMulti && raid.hardCap > i.players ? "(Join to create another group!)" : "");

        int n = 1;
        int p = 0;
        for (ArrayList<RaidMember> group : groups) {
            boolean allBackup = true;
            StringBuilder grouptext = new StringBuilder();
            StringBuilder emojitext = new StringBuilder();
            StringBuilder votetext = new StringBuilder();
            for (RaidMember member : group) {

                boolean isLeader = p % 8 == 0;
                UserSettings us = member.user;

                grouptext.append(cf.getIcon(server, member.getParticipationEmoji(isLeader, i.players >= 24, n)));

                if (!member.isBackup && !member.isMaybe) {
                    allBackup = false;
                }

                if (us.raidcount <= 1) {
                    grouptext.append(cf.getIcon(server, "PlayerSprout"));
                }

                if (us.camera) {
                    grouptext.append(cf.getIcon(server, "FF14Rec"));
                }

                grouptext.append(' ');

                grouptext.append(us.getRealName(server, s -> regenerate(api, server, raid)));

                boolean hasClass = false;
                boolean hasVoting = false;
                if (member.classes.size() > 0) {
                    for (FF14Class clazz : member.classes) {
                        if (clazz.category.equals("VOTING")) {
                            if (clazz.name.equals("1")) {
                                votetext.append("1️");
                            } else if (clazz.name.equals("2")) {
                                votetext.append("2️");
                            } else if (clazz.name.equals("3")) {
                                votetext.append("3️");
                            } else if (clazz.name.equals("4")) {
                                votetext.append("4️");
                            } else if (clazz.name.equals("5")) {
                                votetext.append("5️");
                            }
                            hasVoting = true;
                        } else {
                            emojitext.append(cf.getIcon(server, clazz));
                            hasClass = true;
                        }
                    }
                }
                if (!hasClass) {
                    emojitext.append("-");
                }
                if (!hasVoting) {
                    votetext.append("-");
                }
                emojitext.append("\n");
                grouptext.append("\n");
                votetext.append("\n");
                p++;
            }
            if (allBackup) {
                rm.groups.add(new RaidMessage.Party("Backup Members", grouptext, emojitext, votetext));
            } else {
                rm.groups.add(new RaidMessage.Party(raid.noMulti ? "Party Setup" : "Group " + n, grouptext, emojitext, votetext));
            }
            n++;
        }

        if (raid.isLocked) {
            rm.additionalMessages.put(cf.getIcon(server, "FF14Warning") + " Raid Locked!", "This raid setup has been locked and can currently no longer be changed!");
        }

        if (raid.description.contains("static") || raid.name.contains("static")) {
            rm.additionalMessages.put(cf.getIcon(server, "FF14Question") + " Hildibrand's Reminder:", "Please make sure to have fun and enjoy the game!");
        }

        String ilvlstr = "";
        if (i.level > 0) {
            ilvlstr += "\nRequired Level: " + i.level;
        }
        if (i.ilevel > 0) {
            ilvlstr += "\nRequired avg. item level: " + i.ilevel;
        }

        String specialrules = "";
        if (raid.isNoEcho || raid.isMIL || raid.isUndersized) {
            specialrules = "\n\n" + cf.getIcon(server, "FF14Warning") + " Special rules in effect!\n";
            if (raid.isUndersized) {
                specialrules += "\n" + cf.getIcon(server, "DFSettingUndersized") + " This raid is being done " + cf.getUnderlined("unsynced") + ".";
            }
            if (raid.isMIL) {
                specialrules += "\n" + cf.getIcon(server, "DFSettingMIL") + " This raid is being done with " + cf.getUnderlined("minimum item level") + " turned on.";
            }
            if (raid.isNoEcho) {
                specialrules += "\n" + cf.getIcon(server, "DFSettingNoEcho") + " This raid is being done with the " + cf.getUnderlined("echo silenced") + ".";
            }

        }

        String servertime = sdf.format(raid.time * 1000);

        String length = "";
        if (raid.lengthInHrs > 0) {
            length = "\n" + cf.getInBold("Duration") + ": " + raid.lengthInHrs + " hour" + (raid.lengthInHrs != 1 ? "s" : "");
        }

        rm.header = (gs.notifyRoleId > 0 ? cf.getGroup(gs.notifyRoleId) + " " : "") + "Raid scheduled by " + raid.creator.getRealName(server, s -> regenerate(api, server, raid));
        rm.message = String.format("[#%d] %s\n%s\n\n%s%s: %s\n%s: %s%s%s", raid.runid, cf.getInBold(raid.name), ilvlstr, raid.description.length() > 0 ? String.format("\"%s\"\n\n", raid.description) : "", cf.getInBold("Local Time"), cf.getTimeString(raid.time), cf.getInBold("Server Time"), servertime, length, specialrules);

        return rm;
    }

    public static <ServerType extends Server> RaidListMessage generateRaidListPost(ChatAPI<ServerType> api, ServerType server, List<Raid> raids) {
        ChatFormatter<ServerType> cf = api.getChatFormatter();
        StringBuilder nameSb = new StringBuilder();
        StringBuilder timeSb = new StringBuilder();
        StringBuilder playerSb = new StringBuilder();
        for (Raid r : raids) {
            nameSb.append(r.getNameWithModifierEmojis(cf)).append('\n');
            timeSb.append(cf.getRelativeTimeString(r.time)).append('\n');
            int healer = 0, tank = 0, dps = 0;
            for (RaidMember member : r.getMembers()) {
                for (FF14Class c : member.classes) {
                    switch (c.shortname) {
                        case "TANK":
                            tank++;
                            break;
                        case "HEALER":
                            healer++;
                            break;
                        case "PRDPS":
                        case "MRDPS":
                        case "MDPS":
                            dps++;
                            break;
                    }
                }
            }
            playerSb.append(r.getMemberCount());
            if (dps > 0 || tank > 0 || healer > 0) {
                playerSb.append(" (");
                if (dps > 0) {
                    playerSb.append(dps).append(' ').append(cf.getIcon(server, "MDPS"));
                    if (tank > 0 || healer > 0) {
                        playerSb.append(' ');
                    }
                }
                if (tank > 0) {
                    playerSb.append(tank).append(' ').append(cf.getIcon(server, "TANK"));
                    if (healer > 0) {
                        playerSb.append(' ');
                    }
                }
                if (healer > 0) {
                    playerSb.append(healer).append(' ').append(cf.getIcon(server, "HEALER"));
                }
                playerSb.append(')');
            }
            playerSb.append('\n');
        }

        RaidListMessage rlm = new RaidListMessage(raids);
        rlm.title = "Open Raids (" + raids.size() + ")";
        rlm.description = "You can see here all raids that are currently looking for members:";
        rlm.updatingInfo = "Updates every " + Main.config.settings.timings.raid_list + " minutes. Last updated";
        rlm.names = nameSb;
        rlm.times = timeSb;
        rlm.playercounts = playerSb;

        return rlm;
    }

    private static <ServerType extends Server> void regenerate(ChatAPI<ServerType> api, ServerType server, Raid raid) {
        server.editMessage(raid, MessageGenerator.generateRaidPost(api, server, raid));
    }

}
