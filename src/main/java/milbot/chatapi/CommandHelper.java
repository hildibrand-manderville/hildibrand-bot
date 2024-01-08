package milbot.chatapi;

public class CommandHelper {

    public static void registerAllCommands(Server server) {

        // Common shared options
        CommandParameter RAID_NUMBER_OPTION = new CommandParameter("raid-number", "The number of the run as specified in the raid advertisement", CommandParameter.Type.INT, true);
        CommandParameter CLASS_LIST_OPTION = new CommandParameter("classes", "The class(es) you want to play as. You can use all variations, such as AST, Astrologian or Healer.", CommandParameter.Type.STRING);
        CommandParameter RAID_SETTING_OPTION = new CommandParameter("settings", "Any of: no-self-join, mil, no-echo, unsync, no-notifications, ... For more, check the help page.", CommandParameter.Type.STRING);
        CommandParameter RAID_TIME_OPTION = new CommandParameter("start-time", "The raid starting time in ST. Can be exact (2021-07-20 15:00) or relative (tomorrow 21:00).", CommandParameter.Type.STRING, true);


        // Main commands
        server.registerCommand("join", "Joins the specified raid", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION, CLASS_LIST_OPTION);
        server.registerCommand("joinbackup", "Join a raid in which you want to fill up if it has less than the required amount of players.", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION, CLASS_LIST_OPTION);
        server.registerCommand("joinmaybe", "Join a raid in which you are not 100% sure that you will be able to make it yet.", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION, CLASS_LIST_OPTION);
        server.registerCommand("leave", "Leaves the specified raid", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION, new CommandParameter("stay-in-thread", "Set to true if you want to stay in the raid thread.", CommandParameter.Type.BOOL, false));
        server.registerCommand("set-classes", "Changes your classes/roles for a raid.", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION, CLASS_LIST_OPTION);
        server.registerCommand("save-classes", "Saves your class selection, so it applies to all future raids.", PermissionLevel.EVERYONE, RAID_NUMBER_OPTION);
        server.registerCommand("version", "Show bot version/instance information.", PermissionLevel.EVERYONE);
        //server.registerCommand("hildi", "?", PermissionLevel.EVERYONE);

        server.registerCommand("option/camera", "Displays to other users if you are recording audio and/or video in raids.", PermissionLevel.EVERYONE, new CommandParameter("enable", "true if you will be recording, false if not.", CommandParameter.Type.BOOL, true));
        server.registerCommand("option/notifications", "Enable or disable DM notifications for upcoming raids you're signed up for on this server.", PermissionLevel.EVERYONE, new CommandParameter("enable", "true if you want to receive raid DM notifications from this server, false if not.", CommandParameter.Type.BOOL, true));
        server.registerCommand("option/character-name", "Change your name that is displayed on raid listings.", PermissionLevel.EVERYONE, new CommandParameter("name", "The name to display.", CommandParameter.Type.STRING, true));
        server.registerCommand("option/ping-for-raids", "Set if you want to get pinged for newly created raids.", PermissionLevel.EVERYONE, new CommandParameter("enable", "true to get pinged, false otherwise.", CommandParameter.Type.BOOL, true));

        // Create commands
        server.registerCommand("create", "Creates a raid (creators only)", PermissionLevel.CREATE,
                new CommandParameter("raid-name", "The name of the raid. Can be long name, short name (Shiva EX) or a couple variations of these.", CommandParameter.Type.STRING, true),
                new CommandParameter("start-time", "The raid starting time in ST. Can be exact (2021-07-20 15:00) or relative (tomorrow 21:00).", CommandParameter.Type.STRING, true),
                new CommandParameter("description", "Describe what you're gonna do, if it's gonna be MiL or other modifiers.", CommandParameter.Type.STRING),
                RAID_SETTING_OPTION,
                new CommandParameter("player-cap", "The maximum number of players that can join. Optional and will default to duty size.", CommandParameter.Type.INT),
                new CommandParameter("voting", "If specified, enable voting with the given number of options.", CommandParameter.Type.INT),
                new CommandParameter("length", "If specified, displays for how long you want to raid (in hours).", CommandParameter.Type.INT),
                CLASS_LIST_OPTION
        );
        server.registerCommand("preview", "Previews a raid, and creates it afterwards if submitted. (creators only)", PermissionLevel.CREATE,
                new CommandParameter("raid-name", "The name of the raid. Can be long name, short name (Shiva EX) or a couple variations of these.", CommandParameter.Type.STRING, true),
                RAID_TIME_OPTION,
                new CommandParameter("description", "Describe what you're gonna do, if it's gonna be MiL or other modifiers.", CommandParameter.Type.STRING),
                RAID_SETTING_OPTION,
                new CommandParameter("player-cap", "The maximum number of players that can join. Optional and will default to duty size.", CommandParameter.Type.INT),
                CLASS_LIST_OPTION
        );
        server.registerCommand("clone", "Clones an expired existing raid (creators only)", PermissionLevel.CREATE,
                RAID_NUMBER_OPTION,
                new CommandParameter("new-start-time", "The raid starting time in ST. Can be exact (2021-07-20 15:00) or relative (tomorrow 21:00).", CommandParameter.Type.STRING, true),
                new CommandParameter("clone-players", "True if all players who joined the original will be automatically added to the clone as uncertain.", CommandParameter.Type.BOOL, true),
                new CommandParameter("new-description", "Leave blank to copy the description of the original or add a new description.", CommandParameter.Type.STRING)
        );

        // Raid leader commands
        server.registerCommand("lock", "Locks or unlocks the specified raid. (raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                new CommandParameter("lock", "true if people will no longer be able to join or leave the raid, false to allow. (raid leader only)", CommandParameter.Type.BOOL, true)
        );
        server.registerCommand("modify/add", "Add a player to a raid (ignoring locked status, raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                new CommandParameter("player", "The player to add to that raid.", CommandParameter.Type.USER, true),
                CLASS_LIST_OPTION,
                new CommandParameter("is-backup", "Whether or not this player should be a backup member.", CommandParameter.Type.BOOL, false),
                new CommandParameter("is-maybe", "Whether or not this player should be a uncertain member.", CommandParameter.Type.BOOL, false)
        );
        server.registerCommand("modify/remove", "Remove a player from a raid (ignoring locked status, raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                new CommandParameter("player", "The player to remove from that raid.", CommandParameter.Type.USER, true)
        );
        server.registerCommand("modify/edit-name", "Edits a raid name. This will also re-check if a Lodestone entry can be found. (raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                new CommandParameter("name", "The new raid name.", CommandParameter.Type.STRING, true)
        );
        server.registerCommand("modify/edit-description", "Edits a raid description. (raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                new CommandParameter("new-description", "The new raid description.", CommandParameter.Type.STRING, true)
        );
        server.registerCommand("modify/edit-time", "Edits raid time. (raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                RAID_TIME_OPTION
        );
        server.registerCommand("modify/edit-options", "Edits raid options. (raid leaders only)", PermissionLevel.RAID_LEADER,
                RAID_NUMBER_OPTION,
                RAID_SETTING_OPTION
        );

        // Admin commands
        server.registerCommand("delete", "Deletes a raid (administrators only)", PermissionLevel.ADMIN, RAID_NUMBER_OPTION);
        server.registerCommand("reinitialize", "Refreshes information from the chat service and re-creates command permissions (administrators only)", PermissionLevel.ADMIN);

        server.registerCommand("soption/maybe", "Toggles the \"/joinmaybe\" functionality.", PermissionLevel.ADMIN, new CommandParameter("enable", "true if the maybe option should be enabled, false if not.", CommandParameter.Type.BOOL, true));
        server.registerCommand("soption/allow-unknown-raids", "Change if raids that can't be mapped to a FF14 duty should be allowed.", PermissionLevel.ADMIN, new CommandParameter("allowed", "true if unknown raids are allowed, false if not.", CommandParameter.Type.BOOL, true));
        server.registerCommand("soption/notification-reminder", "Change when people get notified about raids they joined.", PermissionLevel.ADMIN, new CommandParameter("minutes", "Time in minutes when raid members will receive a DM about an upcoming raid (default: 5).", CommandParameter.Type.INT, true));
        server.registerCommand("soption/auto-lock", "Change the time when a raid should be auto-locked.", PermissionLevel.ADMIN, new CommandParameter("minutes", "Time in minutes when a raid will be automatically locked (default: 0, -1 to disable).", CommandParameter.Type.INT, true));
        server.registerCommand("soption/toggle-retains-position", "Change how toggling join status acts.", PermissionLevel.ADMIN, new CommandParameter("retain-position", "true to retain entry position if status is changed, false to put user to the back of the list.", CommandParameter.Type.BOOL, true));
        server.registerCommand("soption/server-icon-shows-raids", "Enable to auto-update the server icon to show a badge with the number of current raids.", PermissionLevel.ADMIN, new CommandParameter("enable", "true to enable feature, false to disable.", CommandParameter.Type.BOOL, true));

        server.registerCommand("soption/set-creator-role", "The role that should have the ability to create raids. Leave blank to disable.", PermissionLevel.ADMIN, new CommandParameter("role", "The role that should have the ability to create raids. Leave blank to disable.", CommandParameter.Type.GROUP));
        server.registerCommand("soption/set-raid-leader-role", "The role that should have the ability to manage raids. Leave blank to disable.", PermissionLevel.ADMIN, new CommandParameter("role", "The role that should have the ability to manage raids. Leave blank to disable.", CommandParameter.Type.GROUP));
        server.registerCommand("soption/set-administrator-role", "The role that should have the ability to manage the bot via /soption. Leave blank to disable.", PermissionLevel.ADMIN, new CommandParameter("role", "The role that should have the ability to manage the bot via /soption. Leave blank to disable.", CommandParameter.Type.GROUP));
        server.registerCommand("soption/set-raidlist-channel", "Set a channel that shows a list of currently open raids. Leave blank to disable.", PermissionLevel.ADMIN, new CommandParameter("list-channel", "The channel that will hold the message.", CommandParameter.Type.ROOM), new CommandParameter("post-channel", "The channel that will be linked to (where people use /create and /join in).", CommandParameter.Type.ROOM));
        server.registerCommand("soption/set-role-for-notifications", "Set a group of users which gets pinged for newly created raids.", PermissionLevel.ADMIN, new CommandParameter("role", "The role getting pinged for raid posts.", CommandParameter.Type.GROUP));
        server.registerCommand("soption/icon-for-tomorrow-raids-only", "Set if the server icon should be updated on all raids or only on tomorrow's.", PermissionLevel.ADMIN, new CommandParameter("enable", "True for tomorrow raids only, false for all.", CommandParameter.Type.BOOL, true));

        server.commitCommands();
    }

}
