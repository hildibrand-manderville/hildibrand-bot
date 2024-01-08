package milbot.objects;

import java.util.ArrayList;

public class Instance {

    public String lsid;
    public String name;
    public int level;
    public int ilevel;
    public int players;
    public InstanceType type;
    public String imageUrl;
    public ArrayList<String> altnames = new ArrayList<>();

    public Instance() {
    }

    public Instance(String lsid, String name, int level, int ilevel, int players, InstanceType type, String imageUrl) {
        this.lsid = lsid;
        this.name = name;
        this.level = level;
        this.ilevel = ilevel;
        this.players = players;
        this.type = type;
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Instance{" +
                "lsid='" + lsid + '\'' +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", ilevel=" + ilevel +
                ", players=" + players +
                ", type=" + type +
                ", imageUrl='" + imageUrl + '\'' +
                ", altnames=" + altnames +
                '}';
    }
}
