package milbot.objects.db;

import eu.haruka.jpsfw.configuration.CSV;

import javax.persistence.*;

@Entity
@Table(name = "classes")
public class FF14Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    public String name;
    public String shortname;
    public String category;
    public boolean showbutton;
    public String altname1;
    public String altname2;

    protected FF14Class() {
    }

    public FF14Class(CSV classes, int row) {
        name = classes.getValue("name", row);
        shortname = classes.getValue("shortname", row);
        category = classes.getValue("category", row);
        showbutton = Boolean.parseBoolean(classes.getValue("showbutton", row));
        altname1 = classes.getValue("altname1", row);
        if (altname1.equals("-")) {
            altname1 = null;
        }
        altname2 = classes.getValue("altname2", row);
        if (altname2.equals("-")) {
            altname2 = null;
        }
    }

    @Override
    public String toString() {
        return shortname;
    }
}
