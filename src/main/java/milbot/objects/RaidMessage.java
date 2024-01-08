package milbot.objects;

import milbot.objects.db.Raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RaidMessage {

    public Raid raid;
    public String header;
    public String playerString;
    public String message;
    public List<Party> groups = new ArrayList<>();
    public HashMap<String, String> additionalMessages = new HashMap<>();
    public Instance instance;

    public RaidMessage(Raid raid) {
        this.raid = raid;
    }

    public static class Party {
        public String title;
        public String names;
        public String classes;
        public String votes;

        public Party(String title, String names, String classes) {
            this.title = title;
            this.names = names;
            this.classes = classes;
        }

        public Party(String title, StringBuilder names, StringBuilder classes, StringBuilder votes) {
            this.title = title;
            this.names = names.toString();
            this.classes = classes.toString();
            this.votes = votes.toString();
        }
    }
}
