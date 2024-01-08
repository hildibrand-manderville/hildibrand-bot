package milbot.objects;

import milbot.Main;

public enum InstanceType {
    DUNGEONS(1, 1, 2),
    TRIALS(2, 2, 4),
    RAIDS(3, 6, 15),
    ULTIMATE_RAIDS(2, 2, 4),
    OTHER(2, 2, 4);

    private final int tanks;
    private final int healers;
    private final int dps;

    InstanceType(int tanks, int healers, int dps) {
        this.tanks = tanks;
        this.healers = healers;
        this.dps = dps;
    }

    public int getTanks() {
        return tanks;
    }

    public int getHealers() {
        return healers;
    }

    public int getDps() {
        return dps;
    }

    public String getIcon() {
        if (this == DUNGEONS) {
            return Main.config.icons.dungeon;
        } else if (this == TRIALS) {
            return Main.config.icons.trial;
        } else if (this == RAIDS) {
            return Main.config.icons.raid;
        } else if (this == ULTIMATE_RAIDS) {
            return Main.config.icons.ultimate_raid;
        } else {
            return Main.config.icons.other;
        }
    }
}
