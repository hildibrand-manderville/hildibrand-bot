package milbot.raid;

import eu.haruka.jpsfw.logging.GlobalLogger;
import milbot.Main;
import milbot.chatapi.ChatAPI;
import milbot.chatapi.Server;
import milbot.lodestone.FF14GameDatabase;
import milbot.objects.RaidListMessage;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.Raid;
import milbot.objects.db.RaidMember;
import milbot.objects.db.UserSettings;
import milbot.util.Util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimedNotifications extends Thread {

    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public TimedNotifications() {
        super("Timed Notifications");
    }

    @Override
    public void run() {
        try {
            while (Main.running) {
                for (GuildSettings gs : FF14GameDatabase.loadAllData(GuildSettings.class)) {
                    if (gs.raidlistRoomId > 0 && System.currentTimeMillis() > gs.lastRaidlistUpdate + Util.minutesToMs(Main.config.settings.timings.raid_list)) {
                        updateRaidListMessage(gs);
                    }
                    if (gs.updateServerIcon && System.currentTimeMillis() > gs.lastIconUpdate + Util.minutesToMs(Main.config.settings.timings.icon_updater)) {
                        updateServerIcon(gs);
                    }
                    for (Raid r : gs.getRaids()) {
                        if (gs.notifyMinutes > 0) {
                            if (!r.isNotified) {
                                if (System.currentTimeMillis() > r.time * 1000 - gs.notifyMinutes * 60L * 1000) {
                                    notifyRaidStart(gs, r);
                                    FF14GameDatabase.save(r);
                                }
                            }
                        }
                        if (gs.autoLockMinutes > -1) {
                            if (!r.isAutoLocked) {
                                if (System.currentTimeMillis() > r.time * 1000 - gs.autoLockMinutes * 60L * 1000) {
                                    autoLockRaid(gs, r);
                                    FF14GameDatabase.save(r);
                                }
                            }
                        }
                        if (!r.isAutoLocked && r.threadid != 0) {
                            Main.getChatAPI().checkThreadState(r, r.threadid);
                        }

                    }
                }
                Date backupdate = new Date();
                String backupfilestr = sdf.format(backupdate) + ".sql";
                File backupfile = new File(Main.BACKUP_DIR, backupfilestr);
                if (!backupfile.exists()) {
                    GlobalLogger.fine("Backuping save...");
                    FF14GameDatabase.getDB().doWork(connection -> {
                        try {
                            connection.prepareStatement("SCRIPT TO '" + backupfile.getCanonicalPath() + "'").execute();
                            GlobalLogger.fine("Backup finished.");
                        } catch (Exception ex) {
                            GlobalLogger.exception(ex, "Backup failed!");
                        }
                    });

                }
                Thread.sleep(1000 * 60);
            }
        } catch (Exception ex) {
            GlobalLogger.exception(ex, "Notification thread crashed");
        }
    }

    private void updateServerIcon(GuildSettings gs) {
        Server s = gs.getServer();
        List<Raid> openRaids = gs.getOpenRaids();
        long tomorrow = System.currentTimeMillis() + Util.hoursToMs(24);
        int raidnum = 0;
        for (Raid r : openRaids) {
            if (!gs.iconOnlyForTomorrowRaids || (tomorrow > r.time * 1000 && !r.isLocked)) {
                raidnum++;
            }
        }

        if (raidnum != gs.lastIconRaidCount) {

            GlobalLogger.info("Updating icon for " + s.getId() + " to " + raidnum + " raid(s)");
            try {
                s.setServerIcon(gs, raidnum);
                gs.lastIconRaidCount = raidnum;
                gs.lastIconUpdate = System.currentTimeMillis();
                FF14GameDatabase.save(gs);
            } catch (Exception ex) {
                GlobalLogger.exception(ex, "Failed to update server icon for: " + s.getId());
            }


        }
    }

    private void updateRaidListMessage(GuildSettings gs) {
        Server s = gs.getServer();
        RaidListMessage rlm = MessageGenerator.generateRaidListPost((ChatAPI<Server>) Main.getChatAPI(), s, gs.getOpenRaids());
        if (gs.raidlistRoomMessage > 0) {
            s.editMessage(gs.raidlistRoomId, gs.raidlistRoomMessage, rlm);
        } else {
            s.postMessage(gs.raidlistRoomId, rlm, messageid -> {
                gs.raidlistRoomMessage = messageid;
                FF14GameDatabase.save(gs);
            });
        }
        gs.lastRaidlistUpdate = System.currentTimeMillis();
        FF14GameDatabase.save(gs);
    }

    public static void autoLockRaid(GuildSettings gs, Raid r) {
        GlobalLogger.info("Auto-locking raid " + r.runid + " on guild " + gs.backendId);
        if (!r.isLocked) {
            r.isLocked = true;
            Server s = gs.getServer();
            if (s != null) {
                s.editMessage(r, MessageGenerator.generateRaidPost((ChatAPI<Server>) Main.getChatAPI(), s, r));
            }
        }
        r.isAutoLocked = true;
    }

    public static void notifyRaidStart(GuildSettings gs, Raid r) {
        GlobalLogger.info("Notifying people for raid " + r.runid + " on guild " + gs.backendId);
        String guild_name;
        Server s = gs.getServer();
        if (s != null) {
            guild_name = s.getName();
        } else {
            guild_name = "<unknown>";
            GlobalLogger.warning("Failed to fetch guild name for: " + gs.backendId + " - unknown guild");
        }
        for (RaidMember rm : r.getMembers()) {
            UserSettings us = rm.user;
            if (us.notifications) {
                GlobalLogger.fine("Notifying: " + us);
                StringBuilder msg = new StringBuilder().append("Upcoming Raid in ").append(gs.notifyMinutes).append(" minute(s): You've signed up for \"").append(r.name).append("\" on server \"").append(guild_name).append('"');
                if (rm.isBackup) {
                    msg.append(" as a backup");
                } else if (rm.isMaybe) {
                    msg.append(" as uncertain if you would be able to join.");
                }
                msg.append('.');
                if (rm.isMaybe || rm.isBackup) {
                    msg.append("\n\nCurrently there are ").append(r.getMemberCount()).append(" player(s) signed up, including you.");
                }
                if (rm.isBackup) {
                    if (r.getMemberCount() <= r.hardCap) {
                        msg.append("\nIt seems like they will probably need you.");
                    }
                } else if (rm.isMaybe) {
                    if (r.getMemberCount() <= r.hardCap) {
                        msg.append("\nPlease tell them if you can come or not.");
                    }
                }
                String post_url = Main.getChatAPI().buildURLToRaidPost(gs.backendId, r.channelid, r.messageid);
                Main.getChatAPI().sendDMTo(us.backendId, msg.toString(), post_url);
            }
        }
        r.isNotified = true;
    }

}
