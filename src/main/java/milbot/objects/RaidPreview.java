package milbot.objects;

import milbot.objects.db.FF14Class;
import milbot.objects.db.Raid;

import java.util.List;

public class RaidPreview {

    public Raid raid;
    public RaidSettings settings;
    public List<FF14Class> classes;

    public RaidPreview(Raid raid, RaidSettings settings, List<FF14Class> classes) {
        this.raid = raid;
        this.settings = settings;
        this.classes = classes;
    }
}
