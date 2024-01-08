package milbot.objects;

import milbot.objects.db.Raid;

import java.util.List;

public class RaidListMessage {

    public String title;
    public String description;
    public String updatingInfo;
    public List<Raid> raids;
    public StringBuilder names;
    public StringBuilder times;
    public StringBuilder playercounts;

    public RaidListMessage(List<Raid> raids) {
        this.raids = raids;
    }
}
